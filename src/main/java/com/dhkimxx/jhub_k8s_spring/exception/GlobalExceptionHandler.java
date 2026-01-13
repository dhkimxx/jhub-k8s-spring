package com.dhkimxx.jhub_k8s_spring.exception;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 처리(Global Exception Handling) 클래스.
 * 애플리케이션 전반에서 발생하는 예외를 포착하여 표준화된 JSON 에러 응답을 반환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 검증 실패 시 발생하는 예외(MethodArgumentNotValidException)를 처리합니다.
     *        첫 번째 필드 에러 메시지를 반환합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");

        return buildResponse(HttpStatus.BAD_REQUEST, message, null);
    }

    /**
     * 쿠버네티스 API 호출 중 발생하는 예외(ApiException)를 처리합니다.
     * API 상태 코드를 그대로 반환하거나, 알 수 없는 경우 500으로 처리합니다.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("Kubernetes API error: {}", ex.getResponseBody(), ex);
        HttpStatus status = HttpStatus.resolve(ex.getCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return buildResponse(status, ex.getResponseBody(), null);
    }

    /**
     * 쿠버네티스 클라이언트 내부 로직 오류(KubernetesClientException)를 처리합니다.
     */
    @ExceptionHandler(KubernetesClientException.class)
    public ResponseEntity<ErrorResponse> handleKubernetesClientException(KubernetesClientException ex) {
        log.error("Kubernetes client error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), null);
    }

    /**
     * 리소스를 찾을 수 없을 때(ResourceNotFoundException) 404를 반환합니다.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    /**
     * 존재하지 않는 엔드포인트 요청 시(NoHandlerFoundException) 404를 반환합니다.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "The requested endpoint was not found.", ex.getRequestURL());
    }

    /**
     * 정적 리소스 요청 실패 시(NoResourceFoundException) 404를 반환합니다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found.", ex.getResourcePath());
    }

    /**
     * 기타 처리되지 않은 모든 예외를 500 Internal Server Error로 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
