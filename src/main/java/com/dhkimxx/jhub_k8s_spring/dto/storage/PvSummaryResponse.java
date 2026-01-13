package com.dhkimxx.jhub_k8s_spring.dto.storage;

import java.util.List;

/**
 * PersistentVolume 요약 정보 DTO.
 * 클러스터에 등록된 PV 정보를 담습니다.
 */
public record PvSummaryResponse(
        String pvName,
        double capacityBytes,
        List<String> accessModes,
        String reclaimPolicy,
        String storageClassName,
        String phase,
        String claimRef) {
}
