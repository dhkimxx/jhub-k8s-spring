package com.dhkimxx.jhub_k8s_spring.controller.view;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionViewController {

    @GetMapping("/sessions")
    public String sessionsPage() {
        // WHY: 세션 페이지는 JS가 API를 호출해 그릴 것이므로 템플릿만 반환한다.
        return "sessions/list";
    }
}
