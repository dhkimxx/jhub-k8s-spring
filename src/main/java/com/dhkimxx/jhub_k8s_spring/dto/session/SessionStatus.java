package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.time.OffsetDateTime;

public record SessionStatus(
        String phase,
        String message,
        OffsetDateTime startTime,
        int restartCount,
        boolean isReady) {
}
