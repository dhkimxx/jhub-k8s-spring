package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.time.OffsetDateTime;

public record PodMetricsResponse(
        String podName,
        OffsetDateTime collectedAt,
        double cpuMilliCores,
        double memoryMiB) {
}
