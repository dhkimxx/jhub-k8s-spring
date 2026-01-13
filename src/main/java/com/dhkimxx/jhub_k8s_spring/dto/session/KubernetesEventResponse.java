package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.time.OffsetDateTime;

public record KubernetesEventResponse(
        String type,
        String reason,
        String message,
        OffsetDateTime eventTime) {
}
