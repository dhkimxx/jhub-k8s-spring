package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.util.List;

/**
 * 사용자 세션 상세 정보 DTO.
 */
public record SessionDetailResponse(
                SessionMetadata metadata,
                SessionStatus status,
                SessionResourceUsage resources,
                List<KubernetesEventResponse> events) {
}
