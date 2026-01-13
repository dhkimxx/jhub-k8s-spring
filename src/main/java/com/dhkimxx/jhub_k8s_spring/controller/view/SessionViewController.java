package com.dhkimxx.jhub_k8s_spring.controller.view;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 세션 관리 페이지 뷰 컨트롤러.
 */
@Controller
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionViewController {

    /** 세션 목록 페이지 렌더링 */
    @GetMapping("/sessions")
    public String sessionsPage() {
        return "sessions/list";
    }
}
