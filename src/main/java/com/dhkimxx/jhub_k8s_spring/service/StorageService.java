package com.dhkimxx.jhub_k8s_spring.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.dhkimxx.jhub_k8s_spring.dto.storage.PvSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.dto.storage.PvcOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.dto.storage.StorageOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPvRepository;
import com.dhkimxx.jhub_k8s_spring.repository.k8s.KubernetesPvcRepository;

import lombok.RequiredArgsConstructor;

/**
 * 클러스터 스토리지 현황을 관리하는 서비스.
 * PV/PVC 정보 조회 및 집계 기능을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageService {

    private final KubernetesPvRepository pvRepository;
    private final KubernetesPvcRepository pvcRepository;

    /**
     * 클러스터 스토리지 개요를 조회합니다.
     * PV/PVC 목록 및 집계 정보를 반환합니다.
     */
    public StorageOverviewResponse fetchStorageOverview() {
        List<PvSummaryResponse> pvList = pvRepository.findAllPvs();
        List<PvcOverviewResponse> pvcList = fetchPvcOverviewList();

        int totalPvCount = pvList.size();
        int boundPvCount = (int) pvList.stream()
                .filter(pv -> "Bound".equals(pv.phase()))
                .count();
        int availablePvCount = (int) pvList.stream()
                .filter(pv -> "Available".equals(pv.phase()))
                .count();

        double totalCapacityBytes = pvList.stream()
                .mapToDouble(PvSummaryResponse::capacityBytes)
                .sum();
        double boundCapacityBytes = pvList.stream()
                .filter(pv -> "Bound".equals(pv.phase()))
                .mapToDouble(PvSummaryResponse::capacityBytes)
                .sum();

        return new StorageOverviewResponse(
                totalPvCount,
                boundPvCount,
                availablePvCount,
                totalCapacityBytes,
                boundCapacityBytes,
                pvcList.size(),
                pvList,
                pvcList);
    }

    /**
     * 네임스페이스 내 PVC 목록을 조회합니다.
     */
    public List<PvcOverviewResponse> fetchPvcOverviewList() {
        return pvcRepository.findAllPvcs().stream()
                .map(pvc -> new PvcOverviewResponse(
                        pvc.pvcName(),
                        pvc.namespace(),
                        pvc.capacityBytes(),
                        pvc.accessModes(),
                        pvc.storageClassName(),
                        pvc.phase(),
                        pvc.volumeName(),
                        null)) // boundPodName은 추가 조회가 필요하므로 일단 null
                .toList();
    }
}
