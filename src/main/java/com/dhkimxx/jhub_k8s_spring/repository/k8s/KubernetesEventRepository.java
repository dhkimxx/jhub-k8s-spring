package com.dhkimxx.jhub_k8s_spring.repository.k8s;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.dhkimxx.jhub_k8s_spring.config.JhubK8sProperties;
import com.dhkimxx.jhub_k8s_spring.dto.session.KubernetesEventResponse;
import com.dhkimxx.jhub_k8s_spring.exception.KubernetesClientException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.EventsV1Api;
import io.kubernetes.client.openapi.models.EventsV1Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KubernetesEventRepository {

    private final EventsV1Api eventsV1Api;
    private final JhubK8sProperties properties;

    public List<KubernetesEventResponse> findEventsByPodName(String podName) {
        try {
            String fieldSelector = "regarding.name=" + podName;
            List<EventsV1Event> events = eventsV1Api.listNamespacedEvent(
                            properties.getNamespace(),
                            null,
                            null,
                            null,
                            fieldSelector,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            Boolean.FALSE)
                    .getItems();

            return events.stream()
                    .map(event -> new KubernetesEventResponse(
                            event.getType(),
                            event.getReason(),
                            event.getNote(),
                            resolveTimestamp(event)))
                    .collect(Collectors.toList());
        } catch (ApiException ex) {
            logApiError("list events for pod " + podName, ex);
            throw new KubernetesClientException(
                    formatApiExceptionMessage("Failed to list events for pod " + podName, ex),
                    ex);
        }
    }

    private OffsetDateTime resolveTimestamp(EventsV1Event event) {
        if (event.getEventTime() != null) {
            return event.getEventTime();
        }
        var series = event.getSeries();
        if (series != null && series.getLastObservedTime() != null) {
            return series.getLastObservedTime();
        }
        var metadata = event.getMetadata();
        return metadata != null ? metadata.getCreationTimestamp() : null;
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
