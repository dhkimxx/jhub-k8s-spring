package com.dhkimxx.jhub_k8s_spring.controller.view;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 클러스터 현황 조회 페이지 뷰 컨트롤러.
 */
@Controller
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterViewController {

    /** 클러스터 노드 현황 페이지 렌더링 */
    @GetMapping("/cluster/nodes")
    public String nodesPage() {
        return "cluster/nodes";
    }
}
