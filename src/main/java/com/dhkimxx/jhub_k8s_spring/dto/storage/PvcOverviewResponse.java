package com.dhkimxx.jhub_k8s_spring.dto.storage;

import java.util.List;

/**
 * PVC 개요 정보 DTO (스토리지 페이지용).
 * PVC 이름, 용량, 상태 등 기본 정보를 담습니다.
 */
public record PvcOverviewResponse(
        String pvcName,
        String namespace,
        double capacityBytes,
        List<String> accessModes,
        String storageClassName,
        String phase,
        String volumeName,
        String boundPodName) {
}
