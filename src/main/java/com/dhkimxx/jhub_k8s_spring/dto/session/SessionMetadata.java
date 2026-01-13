package com.dhkimxx.jhub_k8s_spring.dto.session;

public record SessionMetadata(
        String username,
        String podName,
        String namespace,
        String nodeName) {
}
