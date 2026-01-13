package com.dhkimxx.jhub_k8s_spring.dto.storage;

import java.util.List;

/**
 * 클러스터 스토리지 개요 DTO.
 * 전체 PV 현황 및 집계 정보를 담습니다.
 */
public record StorageOverviewResponse(
        int totalPvCount,
        int boundPvCount,
        int availablePvCount,
        double totalCapacityBytes,
        double boundCapacityBytes,
        int totalPvcCount,
        List<PvSummaryResponse> pvList,
        List<PvcOverviewResponse> pvcList) {
}
