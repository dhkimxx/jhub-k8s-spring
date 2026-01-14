package com.dhkimxx.jhub_k8s_spring.dto.cluster;

/**
 * 클러스터 전체 현황 요약 DTO.
 * 대시보드 상단에 표시될 합계 데이터(노드 수, 세션 수, 전체 리소스 등)를 담습니다.
 */
public record ClusterOverviewResponse(
        int totalNodes,
        int readyNodes,
        int totalSessions,
        int runningSessions,
        double totalCpuCapacityMilliCores,
        double totalCpuAllocatableMilliCores,
        double totalCpuRequestedMilliCores,
        double cpuUsagePercent,
        double totalMemoryCapacityBytes,
        double totalMemoryAllocatableBytes,
        double totalMemoryRequestedBytes,
        double memoryUsagePercent,
        double totalEphemeralStorageCapacityBytes,
        double totalEphemeralStorageAllocatableBytes,
        double totalEphemeralStorageRequestedBytes,
        double ephemeralStorageUsagePercent) {
}
