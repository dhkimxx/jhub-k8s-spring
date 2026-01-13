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

/**
 * 사용자 세션(파드) 관리 API 컨트롤러.
 * 세션 목록 조회, 상세 조회 및 강제 종료 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionApiController {

    private final SessionService sessionService;

    /**
     * 현재 활성화된 모든 사용자 세션 목록을 조회합니다.
     */
    @GetMapping
    public ResponseEntity<List<SessionSummaryResponse>> listSessions() {
        return ResponseEntity.ok(sessionService.fetchAllSessions());
    }

    /**
     * 특정 사용자의 세션 상세 정보를 조회합니다.
     */
    @GetMapping("/{username}")
    public ResponseEntity<SessionDetailResponse> getSessionDetail(@PathVariable String username) {
        return ResponseEntity.ok(sessionService.fetchSessionDetail(username));
    }

    /**
     * 특정 세션(파드)을 별도 스레드에서 비동기로 종료합니다.
     * 종료 요청이 접수되면 202 Accepted를 반환합니다.
     */
    @DeleteMapping("/{podName}")
    public ResponseEntity<Void> terminate(@PathVariable String podName) {
        sessionService.terminateSession(podName);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
