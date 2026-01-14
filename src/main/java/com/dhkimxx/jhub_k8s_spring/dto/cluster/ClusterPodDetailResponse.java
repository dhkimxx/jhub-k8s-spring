package com.dhkimxx.jhub_k8s_spring.dto.cluster;

import java.util.List;
import java.util.Map;

/**
 * 파드 상세 정보 응답 DTO (인프라 관점).
 * 세션 정보가 아닌, 쿠버네티스 리소스 자체의 상세 정보를 담습니다.
 */
public record ClusterPodDetailResponse(
        // Metadata
        String name,
        String namespace,
        String uid,
        String creationTimestamp,
        Map<String, String> labels,
        Map<String, String> annotations,
        String ownerReferenceKind,
        String ownerReferenceName,

        // Spec
        String nodeName,
        String serviceAccountName,
        String restartPolicy,
        String priorityClassName,

        // Status
        String phase,
        String qosClass,
        String podIp,
        String hostIp,
        String startTime,

        // Conditions
        List<PodConditionResponse> conditions,

        // Containers
        List<ContainerDetailResponse> containers) {
    public record PodConditionResponse(
            String type,
            String status,
            String lastProbeTime,
            String lastTransitionTime,
            String reason,
            String message) {
    }

    public record ContainerDetailResponse(
            String name,
            String image,
            String imagePullPolicy,
            boolean ready,
            int restartCount,
            String state, // Running, Waiting, Terminated
            String stateReason,
            String stateMessage,
            // Resources
            double requestCpuMilliCores,
            double limitCpuMilliCores,
            double requestMemoryBytes,
            double limitMemoryBytes,
            // Ports
            List<ContainerPortResponse> ports) {
    }

    public record ContainerPortResponse(
            String name,
            int containerPort,
            String protocol) {
    }
}
