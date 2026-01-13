package com.dhkimxx.jhub_k8s_spring.exception;

/**
 * 요청한 리소스(파드, 노드 등)를 찾을 수 없을 때 발생하는 예외.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
