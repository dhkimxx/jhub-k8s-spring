package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.util.List;

public record SessionDetailResponse(
        SessionSummaryResponse summary,
        PodMetricsResponse metrics,
        List<KubernetesEventResponse> events) {
}
