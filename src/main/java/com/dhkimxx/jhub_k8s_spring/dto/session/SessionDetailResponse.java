package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.util.List;

/**
 * 사용자 세션 상세 정보 DTO.
 * 세션의 요약 정보, 실시간 메트릭, 최근 이벤트 목록을 포함합니다.
 */
public record SessionDetailResponse(
                SessionSummaryResponse summary,
                PodMetricsResponse metrics,
                List<KubernetesEventResponse> events) {
}
