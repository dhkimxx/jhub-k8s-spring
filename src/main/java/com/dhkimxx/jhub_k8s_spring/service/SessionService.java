package com.dhkimxx.jhub_k8s_spring.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.dhkimxx.jhub_k8s_spring.config.JhubK8sProperties;
import com.dhkimxx.jhub_k8s_spring.dto.session.KubernetesEventResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.PodMetricsResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.StorageUsageResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionDetailResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionMetadata;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionResourceUsage;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionStatus;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.exception.ResourceNotFoundException;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesEventRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesMetricsRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPodRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPvcRepository;
import com.dhkimxx.jhub_k8s_spring.util.ResourceQuantityParser;

import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 세션(JupyterHub Pod)을 관리하는 서비스.
 * 파드 조회, 상세 정보(메트릭, 이벤트) 조회 및 세션 종료 기능을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionService {

    private final KubernetesPodRepository podRepository;
    private final KubernetesMetricsRepository metricsRepository;
    private final KubernetesEventRepository eventRepository;
    private final KubernetesPvcRepository pvcRepository;
    private final JhubK8sProperties properties;

    /**
     * 실행 중인 모든 사용자 세션을 요약 정보로 조회합니다.
     * 사용자 이름순으로 정렬하여 반환합니다.
     */
    public List<SessionSummaryResponse> fetchAllSessions() {
        return podRepository.findAllUserPods().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(SessionSummaryResponse::username))
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 세션 상세 정보를 조회합니다.
     * 파드 기본 정보, 실시간 메트릭, 쿠버네티스 이벤트, PVC 정보를 모두 취합합니다.
     */
    public SessionDetailResponse fetchSessionDetail(String username) {
        V1Pod pod = podRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Session for user %s not found".formatted(username)));

        // K8s 활성화 시 추가 정보 조회 (Metrics, Events, Storage)
        PodMetricsResponse metrics = null;
        List<KubernetesEventResponse> events = List.of();
        StorageUsageResponse storage = null;

        if (properties.isEnabled()) {
            metrics = metricsRepository.findPodMetrics(pod.getMetadata().getName()).orElse(null);
            events = eventRepository.findEventsByPodName(pod.getMetadata().getName());
            storage = pvcRepository.findStorageUsageByPod(pod);
        }

        return new SessionDetailResponse(
                toMetadata(pod),
                toStatus(pod),
                toResourceUsage(pod, metrics, storage != null ? storage : StorageUsageResponse.none()),
                events);
    }

    /**
     * 세션(파드)을 강제 종료합니다.
     */
    public void terminateSession(String podName) {
        podRepository.deletePod(podName);
    }

    private SessionSummaryResponse toSummary(V1Pod pod) {
        var metadata = pod.getMetadata();
        var status = pod.getStatus();
        var spec = pod.getSpec();

        String username = "unknown";
        if (metadata != null) {
            var labels = metadata.getLabels();
            if (labels != null) {
                username = labels.getOrDefault(properties.getUsernameLabelKey(), "unknown");
            }
        }
        String phase = status != null ? status.getPhase() : "Unknown";
        boolean ready = isReady(pod);
        int restartCount = aggregateRestarts(pod);
        String nodeName = spec != null ? spec.getNodeName() : "Unknown";
        List<V1Container> containers = spec != null && spec.getContainers() != null
                ? spec.getContainers()
                : List.of();

        double cpuRequests = calculateResource(containers, "cpu", true);
        double memoryRequests = calculateResource(containers, "memory", false);

        return new SessionSummaryResponse(
                username,
                metadata != null ? metadata.getNamespace() : properties.getNamespace(),
                metadata != null ? metadata.getName() : "unknown",
                phase,
                ready,
                restartCount,
                nodeName,
                status != null ? status.getStartTime() : null,
                cpuRequests,
                memoryRequests);
    }

    private SessionMetadata toMetadata(V1Pod pod) {
        String username = "unknown";
        if (pod.getMetadata() != null && pod.getMetadata().getLabels() != null) {
            username = pod.getMetadata().getLabels().getOrDefault(properties.getUsernameLabelKey(), "unknown");
        }
        return new SessionMetadata(
                username,
                pod.getMetadata() != null ? pod.getMetadata().getName() : "unknown",
                pod.getMetadata() != null ? pod.getMetadata().getNamespace() : properties.getNamespace(),
                pod.getSpec() != null ? pod.getSpec().getNodeName() : "Unknown");
    }

    private SessionStatus toStatus(V1Pod pod) {
        String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
        String message = pod.getStatus() != null ? pod.getStatus().getMessage() : null;
        return new SessionStatus(
                phase,
                message,
                pod.getStatus() != null ? pod.getStatus().getStartTime() : null,
                aggregateRestarts(pod),
                isReady(pod));
    }

    private SessionResourceUsage toResourceUsage(V1Pod pod, PodMetricsResponse metrics, StorageUsageResponse storage) {
        double cpuReq = 0.0;
        double cpuLimit = 0.0;
        double memReq = 0.0;
        double memLimit = 0.0;

        if (pod.getSpec() != null && pod.getSpec().getContainers() != null) {
            for (V1Container c : pod.getSpec().getContainers()) {
                if (c.getResources() != null) {
                    if (c.getResources().getRequests() != null) {
                        cpuReq += ResourceQuantityParser.toMilliCores(c.getResources().getRequests().get("cpu"));
                        memReq += ResourceQuantityParser.toBytes(c.getResources().getRequests().get("memory"));
                    }
                    if (c.getResources().getLimits() != null) {
                        cpuLimit += ResourceQuantityParser.toMilliCores(c.getResources().getLimits().get("cpu"));
                        memLimit += ResourceQuantityParser.toBytes(c.getResources().getLimits().get("memory"));
                    }
                }
            }
        }

        Double cpuUsage = metrics != null ? metrics.cpuMilliCores() : null;
        Double memUsage = metrics != null ? metrics.memoryBytes() : null;

        return new SessionResourceUsage(
                new com.dhkimxx.jhub_k8s_spring.dto.session.ResourceItem(cpuReq, cpuLimit > 0 ? cpuLimit : null,
                        cpuUsage),
                new com.dhkimxx.jhub_k8s_spring.dto.session.ResourceItem(memReq, memLimit > 0 ? memLimit : null,
                        memUsage),
                storage);
    }

    private boolean isReady(V1Pod pod) {
        var status = pod.getStatus();
        if (status == null) {
            return false;
        }
        var containerStatuses = status.getContainerStatuses();
        if (containerStatuses == null) {
            return false;
        }
        return containerStatuses.stream().allMatch(V1ContainerStatus::getReady);
    }

    private int aggregateRestarts(V1Pod pod) {
        var status = pod.getStatus();
        if (status == null) {
            return 0;
        }
        var containerStatuses = status.getContainerStatuses();
        if (containerStatuses == null) {
            return 0;
        }
        return containerStatuses.stream()
                .mapToInt(s -> s.getRestartCount() != null ? s.getRestartCount() : 0)
                .sum();
    }

    private double calculateResource(List<V1Container> containers, String key, boolean isCpu) {
        if (containers == null) {
            return 0.0;
        }
        return containers.stream()
                .map(V1Container::getResources)
                .filter(resources -> resources != null)
                .map(resources -> resources.getRequests())
                .filter(requests -> requests != null)
                .map(requests -> requests.get(key))
                .mapToDouble(quantity -> isCpu
                        ? ResourceQuantityParser.toMilliCores(quantity)
                        : ResourceQuantityParser.toBytes(quantity))
                .sum();
    }
}
