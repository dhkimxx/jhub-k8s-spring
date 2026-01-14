package com.dhkimxx.jhub_k8s_spring.dto.cluster;

import java.util.List;
import java.util.Map;

/**
 * 노드 상세 정보 DTO.
 * 특정 노드의 시스템 정보, 네트워크, 상태(Condition), 리소스, 파드 목록 등을 포함합니다.
 */
public record ClusterNodeDetailResponse(
        // Basic Info
        String nodeName,
        String status, // Ready, NotReady, Unknown
        String role, // control-plane, worker
        String creationTimestamp,

        // System Info
        String osImage,
        String kernelVersion,
        String containerRuntimeVersion,
        String kubeletVersion,
        String architecture,
        String operatingSystem,

        // Network
        String internalIp,
        String externalIp,
        String hostname,

        // Metadata
        Map<String, String> labels,
        Map<String, String> annotations,

        // Conditions (Detail)
        List<NodeConditionResponse> conditions,

        // Resources
        double capacityCpuMilliCores,
        double allocatableCpuMilliCores,
        double requestedCpuMilliCores,
        double cpuUsagePercent,

        double capacityMemoryBytes,
        double allocatableMemoryBytes,
        double requestedMemoryBytes,
        double memoryUsagePercent,

        double capacityEphemeralStorageBytes,
        double allocatableEphemeralStorageBytes,
        double requestedEphemeralStorageBytes,
        double ephemeralStorageUsagePercent,

        // Pods
        List<NodePodSummaryResponse> pods) {

    public record NodeConditionResponse(
            String type, // Ready, MemoryPressure, DiskPressure, PIDPressure
            String status, // True, False, Unknown
            String lastHeartbeatTime,
            String lastTransitionTime,
            String reason,
            String message) {
    }

    public record NodePodSummaryResponse(
            String name,
            String namespace,
            String status, // Running, Pending, etc.
            String age,
            double requestedCpuMilliCores,
            double requestedMemoryBytes,
            double requestedEphemeralStorageBytes) {
    }
}
