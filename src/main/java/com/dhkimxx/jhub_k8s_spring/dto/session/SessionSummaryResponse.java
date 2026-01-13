package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.time.OffsetDateTime;

/**
 * 사용자 세션 요약 정보 DTO.
 * 파드의 상태, 할당된 리소스, 재시작 횟수 등 핵심 정보를 담습니다.
 */
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
        double memoryBytes) {
}
