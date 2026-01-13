package com.dhkimxx.jhub_k8s_spring.repository.k8s;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.dhkimxx.jhub_k8s_spring.config.JhubK8sProperties;
import com.dhkimxx.jhub_k8s_spring.exception.KubernetesClientException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KubernetesPodRepository {

    private final CoreV1Api coreV1Api;
    private final JhubK8sProperties properties;

    public List<V1Pod> findAllUserPods() {
        try {
            return coreV1Api.listNamespacedPod(
                            properties.getNamespace(), // namespace
                            null, // pretty
                            null, // allowWatchBookmarks
                            null, // _continue
                            null, // fieldSelector
                            getUserLabelSelector(), // labelSelector
                            properties.getMaxPodFetch(), // limit
                            null, // resourceVersion
                            null, // resourceVersionMatch
                            null, // sendInitialEvents
                            null, // timeoutSeconds
                            Boolean.FALSE) // watch
                    .getItems();
        } catch (ApiException ex) {
            logApiError("list user pods", ex);
            throw new KubernetesClientException(formatApiExceptionMessage("Failed to list user pods", ex), ex);
        }
    }

    public Optional<V1Pod> findByUsername(String username) {
        try {
            String selector = properties.getUsernameLabelKey() + "=" + username;
            List<V1Pod> pods = coreV1Api.listNamespacedPod(
                            properties.getNamespace(),
                            null,
                            null,
                            null,
                            null,
                            selector,
                            1,
                            null,
                            null,
                            null,
                            null,
                            Boolean.FALSE)
                    .getItems();
            return pods.stream().findFirst();
        } catch (ApiException ex) {
            logApiError("find pod by username " + username, ex);
            throw new KubernetesClientException(
                    formatApiExceptionMessage("Failed to find pod for username " + username, ex),
                    ex);
        }
    }

    public List<V1Pod> findByNodeName(String nodeName) {
        try {
            return coreV1Api.listNamespacedPod(
                            properties.getNamespace(),
                            null,
                            null,
                            null,
                            "spec.nodeName=" + nodeName,
                            getUserLabelSelector(),
                            properties.getMaxPodFetch(),
                            null,
                            null,
                            null,
                            null,
                            Boolean.FALSE)
                    .getItems();
        } catch (ApiException ex) {
            logApiError("list pods on node " + nodeName, ex);
            throw new KubernetesClientException(
                    formatApiExceptionMessage("Failed to list pods on node " + nodeName, ex),
                    ex);
        }
    }

    public void deletePod(String podName) {
        try {
            coreV1Api.deleteNamespacedPod(
                    podName,
                    properties.getNamespace(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    new V1DeleteOptions());
        } catch (ApiException ex) {
            logApiError("delete pod " + podName, ex);
            throw new KubernetesClientException(formatApiExceptionMessage("Failed to delete pod " + podName, ex), ex);
        }
    }

    private String getUserLabelSelector() {
        return properties.getUsernameLabelKey();
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
