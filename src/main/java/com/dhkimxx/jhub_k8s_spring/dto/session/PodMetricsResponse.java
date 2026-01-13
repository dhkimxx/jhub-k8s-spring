package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.time.OffsetDateTime;

/**
 * 파드 메트릭 정보 DTO.
 * 실시간 CPU 및 Memory 사용량 데이터를 담습니다.
 */
public record PodMetricsResponse(
        String podName,
        OffsetDateTime collectedAt,
        double cpuMilliCores,
        double memoryBytes) {
}
