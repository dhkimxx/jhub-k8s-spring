package com.dhkimxx.jhub_k8s_spring.controller.api;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.dto.storage.StorageOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.service.ClusterService;

import lombok.RequiredArgsConstructor;

/**
 * 클러스터 정보 조회 API 컨트롤러.
 * 노드 요약 정보 및 전체 클러스터 현황을 JSON으로 반환합니다.
 */
@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterApiController {

    private final ClusterService clusterService;

    /**
     * 클러스터 전체 리소스 현황 및 세션 통계를 조회합니다.
     */
    @GetMapping("/overview")
    public ResponseEntity<ClusterOverviewResponse> overview() {
        return ResponseEntity.ok(clusterService.buildOverview());
    }

    /**
     * 클러스터 내 모든 노드의 요약 목록을 조회합니다.
     */
    @GetMapping("/nodes")
    public ResponseEntity<List<ClusterNodeSummaryResponse>> listNodes() {
        return ResponseEntity.ok(clusterService.fetchNodeSummaries());
    }

    /**
     * 특정 노드의 상세 정보를 조회합니다.
     */
    @GetMapping("/nodes/{nodeName}")
    public ResponseEntity<com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeDetailResponse> getNodeDetail(
            @org.springframework.web.bind.annotation.PathVariable("nodeName") String nodeName) {
        return ResponseEntity.ok(clusterService.fetchNodeDetail(nodeName));
    }

    /**
     * 특정 파드의 인프라 상세 정보를 조회합니다.
     */
    @GetMapping("/pods/{podName}")
    public ResponseEntity<com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterPodDetailResponse> getPodDetail(
            @org.springframework.web.bind.annotation.PathVariable("podName") String podName) {
        return ResponseEntity.ok(clusterService.fetchPodDetail(podName));
    }

    /**
     * 클러스터 스토리지(PV/PVC) 현황을 조회합니다.
     */
    @GetMapping("/storage")
    public ResponseEntity<StorageOverviewResponse> getStorageOverview() {
        return ResponseEntity.ok(clusterService.fetchStorageOverview());
    }
}
