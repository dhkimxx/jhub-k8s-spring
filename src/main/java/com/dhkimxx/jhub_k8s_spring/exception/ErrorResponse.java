package com.dhkimxx.jhub_k8s_spring.exception;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * API 에러 응답 포맷을 정의하는 DTO.
 * 타임스탬프, 상태 코드, 에러 메시지 등을 포함합니다.
 */
@Getter
@Builder
public class ErrorResponse {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
}
