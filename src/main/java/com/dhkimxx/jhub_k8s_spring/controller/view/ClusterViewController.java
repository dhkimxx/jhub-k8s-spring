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

    /** 노드 상세 페이지 렌더링 */
    @GetMapping("/cluster/nodes/{nodeName}")
    public String nodeDetailPage(@org.springframework.web.bind.annotation.PathVariable("nodeName") String nodeName,
            org.springframework.ui.Model model) {
        model.addAttribute("nodeName", nodeName);
        return "cluster/node_detail";
    }

    /** 파드 인프라 상세 페이지 렌더링 */
    @GetMapping("/cluster/pods/{podName}")
    public String podDetailPage(@org.springframework.web.bind.annotation.PathVariable("podName") String podName,
            org.springframework.ui.Model model) {
        model.addAttribute("podName", podName);
        return "cluster/pod_detail";
    }
}
