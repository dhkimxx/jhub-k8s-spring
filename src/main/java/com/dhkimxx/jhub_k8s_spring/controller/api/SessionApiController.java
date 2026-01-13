package com.dhkimxx.jhub_k8s_spring.controller.api;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhkimxx.jhub_k8s_spring.dto.session.SessionDetailResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.SessionSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.service.SessionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionApiController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionSummaryResponse>> listSessions() {
        // WHY: UI는 주기적으로 목록을 갱신하므로 최신 정렬 상태 그대로 전달한다.
        return ResponseEntity.ok(sessionService.fetchAllSessions());
    }

    @GetMapping("/{username}")
    public ResponseEntity<SessionDetailResponse> getSessionDetail(@PathVariable String username) {
        // WHY: 사용자 이름은 포드 라벨과 1:1 매핑이므로 단일 조회로 충분하다.
        return ResponseEntity.ok(sessionService.fetchSessionDetail(username));
    }

    @DeleteMapping("/{podName}")
    public ResponseEntity<Void> terminate(@PathVariable String podName) {
        // WHY: 포드 삭제 요청은 비동기이므로 202 Accepted 로 클러스터 처리 결과를 위임한다.
        sessionService.terminateSession(podName);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
