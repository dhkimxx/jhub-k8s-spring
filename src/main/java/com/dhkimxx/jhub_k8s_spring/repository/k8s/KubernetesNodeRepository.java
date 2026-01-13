package com.dhkimxx.jhub_k8s_spring.repository.k8s;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.dhkimxx.jhub_k8s_spring.exception.KubernetesClientException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KubernetesNodeRepository {

    private final CoreV1Api coreV1Api;

    public List<V1Node> findAllNodes() {
        try {
            return coreV1Api.listNode(
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            Boolean.FALSE)
                    .getItems();
        } catch (ApiException ex) {
            logApiError("list nodes", ex);
            throw new KubernetesClientException(formatApiExceptionMessage("Failed to list cluster nodes", ex), ex);
        }
    }

    private void logApiError(String action, ApiException ex) {
        log.warn(
                "Kubernetes API call [{}] failed. code={}, responseBody={}",
                action,
                ex.getCode(),
                ex.getResponseBody());
    }

    private String formatApiExceptionMessage(String baseMessage, ApiException ex) {
        return baseMessage + " (code=" + ex.getCode() + ", body=" + ex.getResponseBody() + ")";
    }
}
