package com.dhkimxx.jhub_k8s_spring.dto.cluster;

public record ClusterNodeSummaryResponse(
        String nodeName,
        String status,
        String kubeletVersion,
        String osImage,
        double capacityCpuMilliCores,
        double allocatableCpuMilliCores,
        double requestedCpuMilliCores,
        double capacityMemoryMiB,
        double allocatableMemoryMiB,
        double requestedMemoryMiB,
        int runningPodCount) {
}
