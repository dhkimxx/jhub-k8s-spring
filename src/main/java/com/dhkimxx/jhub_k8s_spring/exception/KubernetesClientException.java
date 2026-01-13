package com.dhkimxx.jhub_k8s_spring.exception;

/**
 * 쿠버네티스 클라이언트 연동 중 발생하는 예외.
 * API 호출 실패 외의 클라이언트 설정 오류나 파싱 오류 등을 포함합니다.
 */
public class KubernetesClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KubernetesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
