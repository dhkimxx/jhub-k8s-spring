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

/**
 * 쿠버네티스 노드 정보를 조회하는 리포지토리.
 * 클러스터의 전체 노드 목록을 제공합니다.
 */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KubernetesNodeRepository {

    private final CoreV1Api coreV1Api;

    /**
     * 클러스터 내의 모든 노드를 조회합니다.
     */
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

    /**
     * 특정 노드의 상세 정보를 조회합니다.
     *
     * @param nodeName 조회할 노드 이름
     * @return V1Node 객체 (존재하지 않을 경우 null 반환 가능성 있음 - API 예외 처리 필요)
     */
    public V1Node findNode(String nodeName) {
        try {
            return coreV1Api.readNode(nodeName, null);
        } catch (ApiException ex) {
            logApiError("read node " + nodeName, ex);
            throw new KubernetesClientException(formatApiExceptionMessage("Failed to read node: " + nodeName, ex), ex);
        }
    }
}
