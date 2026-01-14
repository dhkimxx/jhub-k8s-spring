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
import com.dhkimxx.jhub_k8s_spring.dto.storage.PvSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.dto.storage.PvcOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.dto.storage.StorageOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesNodeRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPodRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPvRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPvcRepository;
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
 * 추가적으로 클러스터 전체 스토리지(PV/PVC) 현황도 제공합니다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterService {

        private final KubernetesNodeRepository nodeRepository;
        private final KubernetesPodRepository podRepository;
        private final KubernetesPvRepository pvRepository;
        private final KubernetesPvcRepository pvcRepository;

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
                double totalEphemeralStorageCapacity = nodes.stream()
                                .mapToDouble(ClusterNodeSummaryResponse::capacityEphemeralStorageBytes).sum();
                double totalEphemeralStorageAllocatable = nodes.stream()
                                .mapToDouble(ClusterNodeSummaryResponse::allocatableEphemeralStorageBytes).sum();
                double totalEphemeralStorageRequested = nodes.stream()
                                .mapToDouble(ClusterNodeSummaryResponse::requestedEphemeralStorageBytes).sum();

                double cpuUsagePercent = calculateUsagePercent(totalCpuRequested, totalCpuAllocatable);
                double memoryUsagePercent = calculateUsagePercent(totalMemoryRequested, totalMemoryAllocatable);
                double ephemeralStorageUsagePercent = calculateUsagePercent(totalEphemeralStorageRequested,
                                totalEphemeralStorageAllocatable);

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
                                memoryUsagePercent,
                                totalEphemeralStorageCapacity,
                                totalEphemeralStorageAllocatable,
                                totalEphemeralStorageRequested,
                                ephemeralStorageUsagePercent);
        }

        /**
         * 클러스터 스토리지 개요를 조회합니다.
         * PV/PVC 목록 및 집계 정보를 반환합니다.
         */
        public StorageOverviewResponse fetchStorageOverview() {
                List<PvSummaryResponse> pvList = pvRepository.findAllPvs();
                List<PvcOverviewResponse> pvcList = fetchPvcOverviewList();

                int totalPvCount = pvList.size();
                int boundPvCount = (int) pvList.stream()
                                .filter(pv -> "Bound".equals(pv.phase()))
                                .count();
                int availablePvCount = (int) pvList.stream()
                                .filter(pv -> "Available".equals(pv.phase()))
                                .count();

                double totalCapacityBytes = pvList.stream()
                                .mapToDouble(PvSummaryResponse::capacityBytes)
                                .sum();
                double boundCapacityBytes = pvList.stream()
                                .filter(pv -> "Bound".equals(pv.phase()))
                                .mapToDouble(PvSummaryResponse::capacityBytes)
                                .sum();

                return new StorageOverviewResponse(
                                totalPvCount,
                                boundPvCount,
                                availablePvCount,
                                totalCapacityBytes,
                                boundCapacityBytes,
                                pvcList.size(),
                                pvList,
                                pvcList);
        }

        /**
         * 특정 노드의 상세 정보를 조회합니다.
         */
        public com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse fetchNodeDetail(String nodeName) {
                V1Node node = nodeRepository.findNode(nodeName);
                if (node == null) {
                        throw new IllegalArgumentException("Node not found: " + nodeName);
                }

                List<V1Pod> allPods = podRepository.findAllUserPods();
                List<V1Pod> podsOnNode = allPods.stream()
                                .filter(pod -> {
                                        V1PodSpec spec = pod.getSpec();
                                        return spec != null && nodeName.equals(spec.getNodeName());
                                })
                                .toList();

                return toNodeDetail(node, podsOnNode);
        }

        private com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse toNodeDetail(V1Node node,
                        List<V1Pod> pods) {
                V1NodeStatus status = node.getStatus();
                var metadata = node.getMetadata();
                var nodeInfo = status != null ? status.getNodeInfo() : null;

                // Basic
                String name = metadata != null ? metadata.getName() : "unknown";
                String phase = status != null ? resolveNodeStatus(status) : "Unknown";
                String creationTimestamp = metadata != null && metadata.getCreationTimestamp() != null
                                ? metadata.getCreationTimestamp().toString()
                                : "-";

                // System Info
                String osImage = nodeInfo != null ? nodeInfo.getOsImage() : "-";
                String kernelVersion = nodeInfo != null ? nodeInfo.getKernelVersion() : "-";
                String containerRuntimeVersion = nodeInfo != null ? nodeInfo.getContainerRuntimeVersion() : "-";
                String kubeletVersion = nodeInfo != null ? nodeInfo.getKubeletVersion() : "-";
                String architecture = nodeInfo != null ? nodeInfo.getArchitecture() : "-";
                String operatingSystem = nodeInfo != null ? nodeInfo.getOperatingSystem() : "-";

                // Network Info
                String internalIp = "-";
                String externalIp = "-";
                String hostname = "-";
                if (status != null && status.getAddresses() != null) {
                        for (var addr : status.getAddresses()) {
                                switch (addr.getType()) {
                                        case "InternalIP" -> internalIp = addr.getAddress();
                                        case "ExternalIP" -> externalIp = addr.getAddress();
                                        case "Hostname" -> hostname = addr.getAddress();
                                }
                        }
                }

                // Role
                String role = "worker";
                if (metadata != null && metadata.getLabels() != null) {
                        if (metadata.getLabels().containsKey("node-role.kubernetes.io/control-plane")) {
                                role = "control-plane";
                        }
                }

                // Metadata
                Map<String, String> labels = metadata != null ? metadata.getLabels() : Map.of();
                Map<String, String> annotations = metadata != null ? metadata.getAnnotations() : Map.of();

                // Conditions
                List<com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse.NodeConditionResponse> conditionResponses = List
                                .of();
                if (status != null && status.getConditions() != null) {
                        conditionResponses = status.getConditions().stream()
                                        .map(c -> new com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse.NodeConditionResponse(
                                                        c.getType(),
                                                        c.getStatus(),
                                                        c.getLastHeartbeatTime() != null
                                                                        ? c.getLastHeartbeatTime().toString()
                                                                        : "-",
                                                        c.getLastTransitionTime() != null
                                                                        ? c.getLastTransitionTime().toString()
                                                                        : "-",
                                                        c.getReason(),
                                                        c.getMessage()))
                                        .toList();
                }

                // Resources
                double capacityCpu = 0.0;
                double allocatableCpu = 0.0;
                double capacityMemory = 0.0;
                double allocatableMemory = 0.0;
                double capacityEphemeralStorage = 0.0;
                double allocatableEphemeralStorage = 0.0;

                if (status != null) {
                        Map<String, Quantity> capacity = status.getCapacity();
                        if (capacity != null) {
                                capacityCpu = ResourceQuantityParser.toMilliCores(capacity.get("cpu"));
                                capacityMemory = ResourceQuantityParser.toBytes(capacity.get("memory"));
                                capacityEphemeralStorage = ResourceQuantityParser
                                                .toBytes(capacity.get("ephemeral-storage"));
                        }
                        Map<String, Quantity> allocatable = status.getAllocatable();
                        if (allocatable != null) {
                                allocatableCpu = ResourceQuantityParser.toMilliCores(allocatable.get("cpu"));
                                allocatableMemory = ResourceQuantityParser.toBytes(allocatable.get("memory"));
                                allocatableEphemeralStorage = ResourceQuantityParser
                                                .toBytes(allocatable.get("ephemeral-storage"));
                        }
                }

                double requestedCpu = calculateResourceRequest(pods, "cpu", true);
                double requestedMemory = calculateResourceRequest(pods, "memory", false);
                double requestedEphemeralStorage = calculateResourceRequest(pods, "ephemeral-storage", false);

                double cpuUsagePercent = calculateUsagePercent(requestedCpu, allocatableCpu);
                double memoryUsagePercent = calculateUsagePercent(requestedMemory, allocatableMemory);
                double ephemeralStorageUsagePercent = calculateUsagePercent(requestedEphemeralStorage,
                                allocatableEphemeralStorage);

                // Pod Summary
                List<com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse.NodePodSummaryResponse> podSummaries = pods
                                .stream()
                                .map(pod -> {
                                        String podName = pod.getMetadata().getName();
                                        String namespace = pod.getMetadata().getNamespace();
                                        String podStatus = pod.getStatus() != null ? pod.getStatus().getPhase()
                                                        : "Unknown";
                                        String age = pod.getMetadata().getCreationTimestamp() != null
                                                        ? pod.getMetadata().getCreationTimestamp().toString()
                                                        : "-";

                                        double pCpu = calculateResourceRequest(List.of(pod), "cpu", true);
                                        double pMem = calculateResourceRequest(List.of(pod), "memory", false);
                                        double pStorage = calculateResourceRequest(List.of(pod), "ephemeral-storage",
                                                        false);

                                        return new com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse.NodePodSummaryResponse(
                                                        podName, namespace, podStatus, age, pCpu, pMem, pStorage);
                                })
                                .toList();

                return new com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse(
                                name, phase, role, creationTimestamp,
                                osImage, kernelVersion, containerRuntimeVersion, kubeletVersion, architecture,
                                operatingSystem,
                                internalIp, externalIp, hostname,
                                labels, annotations,
                                conditionResponses,
                                capacityCpu, allocatableCpu, requestedCpu, cpuUsagePercent,
                                capacityMemory, allocatableMemory, requestedMemory, memoryUsagePercent,
                                capacityEphemeralStorage, allocatableEphemeralStorage, requestedEphemeralStorage,
                                ephemeralStorageUsagePercent,
                                podSummaries);
        }

        /**
         * 네임스페이스 내 PVC 목록을 조회합니다.
         */
        public List<PvcOverviewResponse> fetchPvcOverviewList() {
                return pvcRepository.findAllPvcs().stream()
                                .map(pvc -> new PvcOverviewResponse(
                                                pvc.pvcName(),
                                                pvc.namespace(),
                                                pvc.capacityBytes(),
                                                pvc.accessModes(),
                                                pvc.storageClassName(),
                                                pvc.phase(),
                                                pvc.volumeName(),
                                                null))
                                .toList();
        }

        private ClusterNodeSummaryResponse toNodeSummary(V1Node node, List<V1Pod> podsOnNode) {
                V1NodeStatus status = node.getStatus();

                double capacityCpu = 0.0;
                double allocatableCpu = 0.0;
                double capacityMemory = 0.0;
                double allocatableMemory = 0.0;
                double capacityEphemeralStorage = 0.0;
                double allocatableEphemeralStorage = 0.0;

                if (status != null) {
                        Map<String, Quantity> capacity = status.getCapacity();
                        if (capacity != null) {
                                capacityCpu = ResourceQuantityParser.toMilliCores(capacity.get("cpu"));
                                capacityMemory = ResourceQuantityParser.toBytes(capacity.get("memory"));
                                capacityEphemeralStorage = ResourceQuantityParser
                                                .toBytes(capacity.get("ephemeral-storage"));
                        }
                        Map<String, Quantity> allocatable = status.getAllocatable();
                        if (allocatable != null) {
                                allocatableCpu = ResourceQuantityParser.toMilliCores(allocatable.get("cpu"));
                                allocatableMemory = ResourceQuantityParser.toBytes(allocatable.get("memory"));
                                allocatableEphemeralStorage = ResourceQuantityParser
                                                .toBytes(allocatable.get("ephemeral-storage"));
                        }
                }

                double requestedCpu = calculateResourceRequest(podsOnNode, "cpu", true);
                double requestedMemory = calculateResourceRequest(podsOnNode, "memory", false);
                double requestedEphemeralStorage = calculateResourceRequest(podsOnNode, "ephemeral-storage", false);

                double cpuUsagePercent = calculateUsagePercent(requestedCpu, allocatableCpu);
                double memoryUsagePercent = calculateUsagePercent(requestedMemory, allocatableMemory);
                double ephemeralStorageUsagePercent = calculateUsagePercent(requestedEphemeralStorage,
                                allocatableEphemeralStorage);

                String statusMessage = status != null ? resolveNodeStatus(status) : "Unknown";
                String nodeName = Optional.ofNullable(node.getMetadata())
                                .map(m -> m.getName())
                                .orElse("unknown");

                var nodeInfo = status != null ? status.getNodeInfo() : null;

                // Extract InternalIP
                String hostIp = "-";
                if (status != null && status.getAddresses() != null) {
                        for (var addr : status.getAddresses()) {
                                if ("InternalIP".equals(addr.getType())) {
                                        hostIp = addr.getAddress();
                                        break;
                                }
                        }
                }

                return new ClusterNodeSummaryResponse(
                                nodeName,
                                hostIp,
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
                                capacityEphemeralStorage,
                                allocatableEphemeralStorage,
                                requestedEphemeralStorage,
                                ephemeralStorageUsagePercent,
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

        /**
         * 특정 파드의 인프라 레벨 상세 정보를 조회합니다.
         */
        public com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse fetchPodDetail(String podName) {
                V1Pod pod = podRepository.findPod(podName); // Assuming default namespace for now
                if (pod == null) {
                        throw new IllegalArgumentException("Pod not found: " + podName);
                }
                return toPodDetail(pod);
        }

        private com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse toPodDetail(V1Pod pod) {
                var meta = pod.getMetadata();
                var spec = pod.getSpec();
                var status = pod.getStatus();

                // Metadata
                String name = meta.getName();
                String namespace = meta.getNamespace();
                String uid = meta.getUid();
                String creationTimestamp = meta.getCreationTimestamp() != null ? meta.getCreationTimestamp().toString()
                                : "-";

                String ownerKind = "-";
                String ownerName = "-";
                if (meta.getOwnerReferences() != null && !meta.getOwnerReferences().isEmpty()) {
                        var owner = meta.getOwnerReferences().get(0);
                        ownerKind = owner.getKind();
                        ownerName = owner.getName();
                }

                // Spec
                String nodeName = spec != null ? spec.getNodeName() : "-";
                String serviceAccountName = spec != null ? spec.getServiceAccountName() : "-";
                String restartPolicy = spec != null ? spec.getRestartPolicy() : "-";
                String priorityClassName = spec != null ? spec.getPriorityClassName() : "-";

                // Status
                String phase = status != null ? status.getPhase() : "Unknown";
                String qosClass = status != null ? status.getQosClass() : "-";
                String podIp = status != null ? status.getPodIP() : "-";
                String hostIp = status != null ? status.getHostIP() : "-";
                String startTime = status != null && status.getStartTime() != null ? status.getStartTime().toString()
                                : "-";

                // Conditions
                List<com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse.PodConditionResponse> conditions = List
                                .of();
                if (status != null && status.getConditions() != null) {
                        conditions = status.getConditions().stream()
                                        .map(c -> new com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse.PodConditionResponse(
                                                        c.getType(),
                                                        c.getStatus(),
                                                        c.getLastProbeTime() != null ? c.getLastProbeTime().toString()
                                                                        : "-",
                                                        c.getLastTransitionTime() != null
                                                                        ? c.getLastTransitionTime().toString()
                                                                        : "-",
                                                        c.getReason(),
                                                        c.getMessage()))
                                        .toList();
                }

                // Containers
                List<com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse.ContainerDetailResponse> containers = List
                                .of();
                if (spec != null && spec.getContainers() != null) {
                        containers = spec.getContainers().stream().map(c -> {
                                // Find status for this container
                                var cStatus = status != null && status.getContainerStatuses() != null
                                                ? status.getContainerStatuses().stream()
                                                                .filter(cs -> cs.getName().equals(c.getName()))
                                                                .findFirst().orElse(null)
                                                : null;

                                boolean ready = cStatus != null && Boolean.TRUE.equals(cStatus.getReady());
                                int restartCount = cStatus != null ? cStatus.getRestartCount() : 0;

                                String state = "Unknown";
                                String stateReason = null;
                                String stateMessage = null;

                                if (cStatus != null && cStatus.getState() != null) {
                                        if (cStatus.getState().getRunning() != null) {
                                                state = "Running";
                                                stateReason = "Started at "
                                                                + cStatus.getState().getRunning().getStartedAt();
                                        } else if (cStatus.getState().getWaiting() != null) {
                                                state = "Waiting";
                                                stateReason = cStatus.getState().getWaiting().getReason();
                                                stateMessage = cStatus.getState().getWaiting().getMessage();
                                        } else if (cStatus.getState().getTerminated() != null) {
                                                state = "Terminated";
                                                stateReason = cStatus.getState().getTerminated().getReason();
                                                stateMessage = cStatus.getState().getTerminated().getMessage();
                                        }
                                }

                                // Resources
                                double reqCpu = 0;
                                double limitCpu = 0;
                                double reqMem = 0;
                                double limitMem = 0;
                                if (c.getResources() != null) {
                                        if (c.getResources().getRequests() != null) {
                                                reqCpu = ResourceQuantityParser.toMilliCores(
                                                                c.getResources().getRequests().get("cpu"));
                                                reqMem = ResourceQuantityParser
                                                                .toBytes(c.getResources().getRequests().get("memory"));
                                        }
                                        if (c.getResources().getLimits() != null) {
                                                limitCpu = ResourceQuantityParser
                                                                .toMilliCores(c.getResources().getLimits().get("cpu"));
                                                limitMem = ResourceQuantityParser
                                                                .toBytes(c.getResources().getLimits().get("memory"));
                                        }
                                }

                                // Ports
                                List<com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse.ContainerPortResponse> ports = List
                                                .of();
                                if (c.getPorts() != null) {
                                        ports = c.getPorts().stream()
                                                        .map(p -> new com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse.ContainerPortResponse(
                                                                        p.getName(),
                                                                        p.getContainerPort(),
                                                                        p.getProtocol()))
                                                        .toList();
                                }

                                return new com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse.ContainerDetailResponse(
                                                c.getName(),
                                                c.getImage(),
                                                c.getImagePullPolicy(),
                                                ready,
                                                restartCount,
                                                state,
                                                stateReason,
                                                stateMessage,
                                                reqCpu, limitCpu, reqMem, limitMem,
                                                ports);
                        }).toList();
                }

                return new com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse(
                                name, namespace, uid, creationTimestamp,
                                meta.getLabels(), meta.getAnnotations(),
                                ownerKind, ownerName,
                                nodeName, serviceAccountName, restartPolicy, priorityClassName,
                                phase, qosClass, podIp, hostIp, startTime,
                                conditions,
                                containers);
        }

        private double calculateUsagePercent(double requested, double allocatable) {
                if (allocatable <= 0) {
                        return 0.0;
                }
                return (requested / allocatable) * 100.0;
        }
}
