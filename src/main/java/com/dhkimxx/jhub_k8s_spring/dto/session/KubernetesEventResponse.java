package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.time.OffsetDateTime;

/**
 * 쿠버네티스 이벤트 정보 DTO.
 * 파드와 관련된 주요 이벤트(Created, Started, Failed 등)를 담습니다.
 */
public record KubernetesEventResponse(
                String type,
                String reason,
                String message,
                OffsetDateTime eventTime) {
}
