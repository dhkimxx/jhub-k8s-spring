package com.dhkimxx.jhub_k8s_spring.dto.session;

import java.util.List;

/**
 * PVC(PersistentVolumeClaim) 요약 정보 DTO.
 * 사용자 세션에 연결된 스토리지 볼륨 정보를 담습니다.
 */
public record PvcSummaryResponse(
        String pvcName,
        String namespace,
        double capacityBytes,
        List<String> accessModes,
        String storageClassName,
        String phase,
        String volumeName) {
}
