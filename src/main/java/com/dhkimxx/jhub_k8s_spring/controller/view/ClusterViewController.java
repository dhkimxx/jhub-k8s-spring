package com.dhkimxx.jhub_k8s_spring.controller.view;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterViewController {

    @GetMapping("/cluster/nodes")
    public String nodesPage() {
        // WHY: 노드 현황 페이지도 데이터는 API에서 가져오므로 템플릿만 반환한다.
        return "cluster/nodes";
    }
}
