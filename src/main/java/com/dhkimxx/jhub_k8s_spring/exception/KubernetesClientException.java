package com.dhkimxx.jhub_k8s_spring.exception;

public class KubernetesClientException extends RuntimeException {

    public KubernetesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
