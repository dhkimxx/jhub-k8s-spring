package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.time.OffsetDateTime;

public record SessionSummaryResponse(
        String username,
        String namespace,
        String podName,
        String phase,
        boolean ready,
        int restartCount,
        String nodeName,
        OffsetDateTime startTime,
        double cpuMilliCores,
        double memoryMiB) {
}
