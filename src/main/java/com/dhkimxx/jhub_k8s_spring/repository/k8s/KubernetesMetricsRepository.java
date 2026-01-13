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
