package com.dhkimxx.jhub_k8s_spring.repository.k8s;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.dhkimxx.jhub_k8s_spring.config.JhubK8sProperties;
import com.dhkimxx.jhub_k8s_spring.dto.session.PodMetricsResponse;
import com.dhkimxx.jhub_k8s_spring.exception.KubernetesClientException;
import com.dhkimxx.jhub_k8s_spring.util.ResourceQuantityParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import lombok.RequiredArgsConstructor;

/**
 * 쿠버네티스 파드 메트릭(metrics.k8s.io)을 조회하는 리포지토리.
 * CustomObjectsApi를 사용하여 원시 메트릭 데이터를 가져와 파싱합니다.
 */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KubernetesMetricsRepository {

    private static final String METRICS_GROUP = "metrics.k8s.io";
    private static final String METRICS_VERSION = "v1beta1";
    private static final String METRICS_RESOURCE = "pods";

    private final CustomObjectsApi customObjectsApi;
    private final JhubK8sProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 특정 파드의 실시간 CPU 및 메모리 사용량을 조회합니다.
     * Metrics Server가 설치되어 있어야 동작합니다.
     */
    public Optional<PodMetricsResponse> findPodMetrics(String podName) {
        try {
            Object response = customObjectsApi.getNamespacedCustomObject(
                    METRICS_GROUP,
                    METRICS_VERSION,
                    properties.getNamespace(),
                    METRICS_RESOURCE,
                    podName);

            JsonNode root = objectMapper.valueToTree(response);
            if (root == null || !root.has("containers")) {
                return Optional.empty();
            }

            Iterator<JsonNode> containers = root.path("containers").elements();
            double cpu = 0d;
            double memory = 0d;
            while (containers.hasNext()) {
                JsonNode usageNode = containers.next().path("usage");
                cpu += ResourceQuantityParser.toMilliCores(usageNode.path("cpu").asText(""));
                memory += ResourceQuantityParser.toMiB(usageNode.path("memory").asText(""));
            }

            OffsetDateTime timestamp = root.hasNonNull("timestamp")
                    ? OffsetDateTime.parse(root.get("timestamp").asText())
                    : OffsetDateTime.now();

            return Optional.of(new PodMetricsResponse(podName, timestamp, cpu, memory));
        } catch (ApiException ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            throw new KubernetesClientException("Failed to parse pod metrics for " + podName, ex);
        }
    }
}
