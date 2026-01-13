package com.dhkimxx.jhub_k8s_spring.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 스토리지 페이지 뷰 컨트롤러.
 * /storage 경로의 HTML 페이지를 렌더링합니다.
 */
@Controller
@RequestMapping("/cluster/storage")
public class StorageViewController {

    /**
     * 스토리지 모니터링 페이지를 반환합니다.
     */
    @GetMapping
    public String storagePage() {
        return "storage/index";
    }
}
