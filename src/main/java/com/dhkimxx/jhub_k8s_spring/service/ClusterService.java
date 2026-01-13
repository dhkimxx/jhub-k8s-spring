package com.dhkimxx.jhub_k8s_spring.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesNodeRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPodRepository;
import com.dhkimxx.jhub_k8s_spring.util.ResourceQuantityParser;

import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterService {

    private final KubernetesNodeRepository nodeRepository;
    private final KubernetesPodRepository podRepository;

    public List<ClusterNodeSummaryResponse> fetchNodeSummaries() {
        List<V1Pod> pods = podRepository.findAllUserPods();
        Map<String, List<V1Pod>> podsByNode = pods.stream()
                .filter(pod -> pod.getSpec() != null && pod.getSpec().getNodeName() != null)
                .collect(Collectors.groupingBy(pod -> pod.getSpec().getNodeName()));

        return nodeRepository.findAllNodes().stream()
                .map(node -> toNodeSummary(node, podsByNode.getOrDefault(node.getMetadata().getName(), List.of())))
                .sorted(Comparator.comparing(ClusterNodeSummaryResponse::nodeName))
                .collect(Collectors.toList());
    }

    public ClusterOverviewResponse buildOverview() {
        List<ClusterNodeSummaryResponse> nodes = fetchNodeSummaries();
        int totalSessions = podRepository.findAllUserPods().size();
        long runningSessions = podRepository.findAllUserPods().stream()
                .filter(pod -> pod.getStatus() != null && "Running".equalsIgnoreCase(pod.getStatus().getPhase()))
                .count();

        double totalCpuCapacity = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::capacityCpuMilliCores).sum();
        double totalCpuAllocatable = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::allocatableCpuMilliCores).sum();
        double totalCpuRequested = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::requestedCpuMilliCores).sum();
        double totalMemoryCapacity = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::capacityMemoryMiB).sum();
        double totalMemoryAllocatable = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::allocatableMemoryMiB).sum();
        double totalMemoryRequested = nodes.stream().mapToDouble(ClusterNodeSummaryResponse::requestedMemoryMiB).sum();

        return new ClusterOverviewResponse(
                nodes.size(),
                (int) nodes.stream().filter(node -> "Ready".equals(node.status())).count(),
                totalSessions,
                (int) runningSessions,
                totalCpuCapacity,
                totalCpuAllocatable,
                totalCpuRequested,
                totalMemoryCapacity,
                totalMemoryAllocatable,
                totalMemoryRequested);
    }

    private ClusterNodeSummaryResponse toNodeSummary(V1Node node, List<V1Pod> podsOnNode) {
        V1NodeStatus status = node.getStatus();
        double capacityCpu = ResourceQuantityParser.toMilliCores(status.getCapacity().get("cpu"));
        double allocatableCpu = ResourceQuantityParser.toMilliCores(status.getAllocatable().get("cpu"));
        double capacityMemory = ResourceQuantityParser.toMiB(status.getCapacity().get("memory"));
        double allocatableMemory = ResourceQuantityParser.toMiB(status.getAllocatable().get("memory"));

        double requestedCpu = podsOnNode.stream()
                .flatMap(pod -> pod.getSpec().getContainers().stream())
                .map(container -> container.getResources())
                .filter(resources -> resources != null && resources.getRequests() != null)
                .map(resources -> resources.getRequests().get("cpu"))
                .mapToDouble(ResourceQuantityParser::toMilliCores)
                .sum();
        double requestedMemory = podsOnNode.stream()
                .flatMap(pod -> pod.getSpec().getContainers().stream())
                .map(container -> container.getResources())
                .filter(resources -> resources != null && resources.getRequests() != null)
                .map(resources -> resources.getRequests().get("memory"))
                .mapToDouble(ResourceQuantityParser::toMiB)
                .sum();

        String statusMessage = resolveNodeStatus(status);
        return new ClusterNodeSummaryResponse(
                node.getMetadata().getName(),
                statusMessage,
                status.getNodeInfo().getKubeletVersion(),
                status.getNodeInfo().getOsImage(),
                capacityCpu,
                allocatableCpu,
                requestedCpu,
                capacityMemory,
                allocatableMemory,
                requestedMemory,
                podsOnNode.size());
    }

    private String resolveNodeStatus(V1NodeStatus status) {
        return status.getConditions().stream()
                .filter(condition -> "Ready".equalsIgnoreCase(condition.getType()))
                .map(V1NodeCondition::getStatus)
                .findFirst()
                .map(value -> "True".equalsIgnoreCase(value) ? "Ready" : "NotReady")
                .orElse("Unknown");
    }
}
