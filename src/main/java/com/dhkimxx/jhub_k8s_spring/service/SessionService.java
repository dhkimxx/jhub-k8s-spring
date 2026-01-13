package com.dhkimxx.jhub_k8s_spring.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.dhkimxx.jhub_k8s_spring.config.JhubK8sProperties;
import com.dhkimxx.jhub_k8s_spring.dto.session.KubernetesEventResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.PodMetricsResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionDetailResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.exception.ResourceNotFoundException;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesEventRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesMetricsRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPodRepository;
import com.dhkimxx.jhub_k8s_spring.util.ResourceQuantityParser;

import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionService {

    private final KubernetesPodRepository podRepository;
    private final KubernetesMetricsRepository metricsRepository;
    private final KubernetesEventRepository eventRepository;
    private final JhubK8sProperties properties;

    public List<SessionSummaryResponse> fetchAllSessions() {
        return podRepository.findAllUserPods().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(SessionSummaryResponse::username))
                .collect(Collectors.toList());
    }

    public SessionDetailResponse fetchSessionDetail(String username) {
        V1Pod pod = podRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Session for user %s not found".formatted(username)));

        SessionSummaryResponse summary = toSummary(pod);
        PodMetricsResponse metrics = metricsRepository.findPodMetrics(summary.podName())
                .orElseGet(() -> new PodMetricsResponse(summary.podName(), summary.startTime(), summary.cpuMilliCores(), summary.memoryMiB()));
        List<KubernetesEventResponse> events = eventRepository.findEventsByPodName(summary.podName());
        return new SessionDetailResponse(summary, metrics, events);
    }

    public void terminateSession(String podName) {
        podRepository.deletePod(podName);
    }

    private SessionSummaryResponse toSummary(V1Pod pod) {
        var metadata = pod.getMetadata();
        var status = pod.getStatus();
        var spec = pod.getSpec();

        String username = Optional.ofNullable(metadata)
                .map(m -> m.getLabels())
                .map(labels -> labels.get(properties.getUsernameLabelKey()))
                .orElse("unknown");
        String phase = status != null ? status.getPhase() : "Unknown";
        boolean ready = isReady(pod);
        int restartCount = aggregateRestarts(pod);
        String nodeName = spec != null ? spec.getNodeName() : "Unknown";
        List<V1Container> containers = spec != null && spec.getContainers() != null
                ? spec.getContainers()
                : List.of();

        double cpuRequests = aggregateResource(containers, "cpu", true);
        double memoryRequests = aggregateResource(containers, "memory", false);

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

    private boolean isReady(V1Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return false;
        }
        return pod.getStatus().getContainerStatuses().stream().allMatch(V1ContainerStatus::getReady);
    }

    private int aggregateRestarts(V1Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return 0;
        }
        return pod.getStatus().getContainerStatuses().stream()
                .mapToInt(status -> status.getRestartCount() != null ? status.getRestartCount() : 0)
                .sum();
    }

    private double aggregateResource(List<V1Container> containers, String key, boolean cpu) {
        return containers.stream()
                .map(container -> container.getResources())
                .filter(resources -> resources != null && resources.getRequests() != null)
                .map(resources -> resources.getRequests().get(key))
                .mapToDouble(quantity -> cpu
                        ? ResourceQuantityParser.toMilliCores(quantity)
                        : ResourceQuantityParser.toMiB(quantity))
                .sum();
    }
}
