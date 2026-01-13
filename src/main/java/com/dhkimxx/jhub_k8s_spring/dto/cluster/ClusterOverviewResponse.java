package com.dhkimxx.jhub_k8s_spring.dto.cluster;

public record ClusterOverviewResponse(
        int totalNodes,
        int readyNodes,
        int totalSessions,
        int runningSessions,
        double totalCpuCapacityMilliCores,
        double totalCpuAllocatableMilliCores,
        double totalCpuRequestedMilliCores,
        double totalMemoryCapacityMiB,
        double totalMemoryAllocatableMiB,
        double totalMemoryRequestedMiB) {
}
