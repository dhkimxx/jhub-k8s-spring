package com.dhkimxx.jhub_k8s_spring.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesNodeRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPodRepository;
import com.dhkimxx.jhub_k8s_spring.util.ResourceQuantityParser;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;

import io.kubernetes.client.openapi.models.V1Node;

import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import lombok.RequiredArgsConstructor;

/**
 * 클러스터 노드 및 리소스 상태를 집계하는 서비스.
 * 노드별 리소스 사용량(CPU, Memory)과 파드 배치 현황을 분석합니다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterService {

        private final KubernetesNodeRepository nodeRepository;
        private final KubernetesPodRepository podRepository;

        /**
         * 전체 노드의 리소스 요약 정보를 조회합니다.
         * 각 노드에 배치된 파드들의 리소스 요청량을 합산하여 계산합니다.
         */
        public List<ClusterNodeSummaryResponse> fetchNodeSummaries() {
                List<V1Pod> pods = podRepository.findAllUserPods();
                Map<String, List<V1Pod>> podsByNode = pods.stream()
                                .filter(pod -> {
                                        V1PodSpec spec = pod.getSpec();
                                        return spec != null && spec.getNodeName() != null;
                                })
                                .collect(Collectors.groupingBy(pod -> {
                                        V1PodSpec spec = pod.getSpec();
                                        // filter에서 이미 체크했으나, 정적 분석기를 만족시키기 위해 삼항 연산자 사용
                                        return spec != null ? spec.getNodeName() : "unknown";
                                }));

                return nodeRepository.findAllNodes().stream()
                                .map(node -> {
                                        String nodeName = Optional.ofNullable(node.getMetadata())
                                                        .map(m -> m.getName())
                                                        .orElse("unknown");
                                        return toNodeSummary(node, podsByNode.getOrDefault(nodeName, List.of()));
                                })
                                .sorted(Comparator.comparing(ClusterNodeSummaryResponse::nodeName))
                                .collect(Collectors.toList());
        }

        /**
         * 클러스터 전체 현황(대시보드 상단 요약)을 생성합니다.
         * 전체 세션(파드) 수, 실행 중인 세션 수, 전체 리소스 용량 등을 집계합니다.
         */
        public ClusterOverviewResponse buildOverview() {
                List<ClusterNodeSummaryResponse> nodes = fetchNodeSummaries();
                int totalSessions = podRepository.findAllUserPods().size();
                long runningSessions = podRepository.findAllUserPods().stream()
                                .map(V1Pod::getStatus)
                                .filter(status -> status != null && "Running".equalsIgnoreCase(status.getPhase()))
                                .count();

                double totalCpuCapacity = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::capacityCpuMilliCores)
                                .sum();
                double totalCpuAllocatable = nodes.stream()
                                .mapToDouble(ClusterNodeSummaryResponse::allocatableCpuMilliCores).sum();
                double totalCpuRequested = nodes.stream()
                                .mapToDouble(ClusterNodeSummaryResponse::requestedCpuMilliCores).sum();
                double totalMemoryCapacity = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::capacityMemoryBytes)
                                .sum();
                double totalMemoryAllocatable = nodes.stream()
                                .mapToDouble(ClusterNodeSummaryResponse::allocatableMemoryBytes).sum();
                double totalMemoryRequested = nodes.stream()
                                .mapToDouble(ClusterNodeSummaryResponse::requestedMemoryBytes)
                                .sum();

                double cpuUsagePercent = calculateUsagePercent(totalCpuRequested, totalCpuAllocatable);
                double memoryUsagePercent = calculateUsagePercent(totalMemoryRequested, totalMemoryAllocatable);

                return new ClusterOverviewResponse(
                                nodes.size(),
                                (int) nodes.stream().filter(node -> "Ready".equals(node.status())).count(),
                                totalSessions,
                                (int) runningSessions,
                                totalCpuCapacity,
                                totalCpuAllocatable,
                                totalCpuRequested,
                                cpuUsagePercent,
                                totalMemoryCapacity,
                                totalMemoryAllocatable,
                                totalMemoryRequested,
                                memoryUsagePercent);
        }

        private ClusterNodeSummaryResponse toNodeSummary(V1Node node, List<V1Pod> podsOnNode) {
                V1NodeStatus status = node.getStatus();

                double capacityCpu = 0.0;
                double allocatableCpu = 0.0;
                double capacityMemory = 0.0;
                double allocatableMemory = 0.0;

                if (status != null) {
                        Map<String, Quantity> capacity = status.getCapacity();
                        if (capacity != null) {
                                capacityCpu = ResourceQuantityParser.toMilliCores(capacity.get("cpu"));
                                capacityMemory = ResourceQuantityParser.toBytes(capacity.get("memory"));
                        }
                        Map<String, Quantity> allocatable = status.getAllocatable();
                        if (allocatable != null) {
                                allocatableCpu = ResourceQuantityParser.toMilliCores(allocatable.get("cpu"));
                                allocatableMemory = ResourceQuantityParser.toBytes(allocatable.get("memory"));
                        }
                }

                double requestedCpu = calculateResourceRequest(podsOnNode, "cpu", true);
                double requestedMemory = calculateResourceRequest(podsOnNode, "memory", false);

                double cpuUsagePercent = calculateUsagePercent(requestedCpu, allocatableCpu);
                double memoryUsagePercent = calculateUsagePercent(requestedMemory, allocatableMemory);

                String statusMessage = status != null ? resolveNodeStatus(status) : "Unknown";
                String nodeName = Optional.ofNullable(node.getMetadata())
                                .map(m -> m.getName())
                                .orElse("unknown");

                var nodeInfo = status != null ? status.getNodeInfo() : null;

                return new ClusterNodeSummaryResponse(
                                nodeName,
                                statusMessage,
                                nodeInfo != null ? nodeInfo.getKubeletVersion() : "unknown",
                                nodeInfo != null ? nodeInfo.getOsImage() : "unknown",
                                capacityCpu,
                                allocatableCpu,
                                requestedCpu,
                                cpuUsagePercent,
                                capacityMemory,
                                allocatableMemory,
                                requestedMemory,
                                memoryUsagePercent,
                                podsOnNode.size());
        }

        private String resolveNodeStatus(V1NodeStatus status) {
                List<V1NodeCondition> conditions = status.getConditions();
                if (conditions == null) {
                        return "Unknown";
                }
                return conditions.stream()
                                .filter(condition -> "Ready".equalsIgnoreCase(condition.getType()))
                                .map(V1NodeCondition::getStatus)
                                .findFirst()
                                .map(value -> "True".equalsIgnoreCase(value) ? "Ready" : "NotReady")
                                .orElse("Unknown");
        }

        private double calculateResourceRequest(List<V1Pod> pods, String resourceKey, boolean isCpu) {
                return pods.stream()
                                .flatMap(this::getSafeContainerStream)
                                .map(V1Container::getResources)
                                .filter(res -> res != null)
                                .map(res -> res.getRequests())
                                .filter(requests -> requests != null)
                                .map(requests -> requests.get(resourceKey))
                                .mapToDouble(quantity -> isCpu ? ResourceQuantityParser.toMilliCores(quantity)
                                                : ResourceQuantityParser.toBytes(quantity))
                                .sum();
        }

        private Stream<V1Container> getSafeContainerStream(V1Pod pod) {
                if (pod == null) {
                        return Stream.empty();
                }
                V1PodSpec spec = pod.getSpec();
                if (spec == null || spec.getContainers() == null) {
                        return Stream.empty();
                }
                return spec.getContainers().stream();
        }

        /**
         * 사용률(%) 계산.
         * allocatable이 0인 경우 0%를 반환하여 Division by zero 방지.
         */
        private double calculateUsagePercent(double requested, double allocatable) {
                if (allocatable <= 0) {
                        return 0.0;
                }
                return (requested / allocatable) * 100.0;
        }
}
