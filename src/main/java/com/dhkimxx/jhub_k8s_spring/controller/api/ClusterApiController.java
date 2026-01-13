package com.dhkimxx.jhub_k8s_spring.controller.api;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterNodeSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.dto.cluster.ClusterOverviewResponse;
import com.dhkimxx.jhub_k8s_spring.service.ClusterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterApiController {

    private final ClusterService clusterService;

    @GetMapping("/nodes")
    public ResponseEntity<List<ClusterNodeSummaryResponse>> listNodes() {
        // WHY: 노드 카드 UI에서 정렬된 노드 목록을 필요로 하므로 서비스 결과를 그대로 내려준다.
        return ResponseEntity.ok(clusterService.fetchNodeSummaries());
    }

    @GetMapping("/overview")
    public ResponseEntity<ClusterOverviewResponse> overview() {
        // WHY: 상단 KPI 패널이 개요 카드 단일 호출로 계산되도록 별도 엔드포인트를 둔다.
        return ResponseEntity.ok(clusterService.buildOverview());
    }
}
