package com.dhkimxx.jhub_k8s_spring.controller.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhkimxx.jhub_k8s_spring.dto.storage.StorageOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.service.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 스토리지 관련 API 컨트롤러.
 * PV/PVC 현황 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "스토리지(PV/PVC) 현황 API")
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageApiController {

    private final StorageService storageService;

    /**
     * 클러스터 스토리지 개요를 조회합니다.
     */
    @GetMapping("/overview")
    @Operation(summary = "스토리지 개요 조회", description = "클러스터의 PV/PVC 현황 및 집계 정보를 반환합니다")
    public StorageOverviewResponse getStorageOverview() {
        return storageService.fetchStorageOverview();
    }
}
