package com.dhkimxx.jhub_k8s_spring.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 쿠버네티스 연동을 위한 설정 속성 정의 클래스.
 * application.properties의 'jhub.k8s' 접두사를 가진 설정값들을 매핑합니다.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jhub.k8s")
public class JhubK8sProperties {

    /** 타겟 쿠버네티스 네임스페이스 */
    @NotBlank
    private String namespace;

    /** 사용자 식별을 위한 라벨 키 (예: hub.jupyter.org/username) */
    @NotBlank
    private String usernameLabelKey;

    @NotBlank
    private String podNamePrefix;

    private boolean useKubeconfig = false;

    private String apiServerUrl;

    private String bearerToken;

    /** Kubeconfig 파일 경로 */
    @NotBlank
    private String kubeconfigPath;

    private boolean verifySsl = true;

    @NotNull
    private Duration requestTimeout;

    @NotNull
    private Duration metricsTimeout;

    private boolean defaultNamespaceSelector = true;

    @Min(1)
    @Max(1000)
    private int maxPodFetch = 200;
}
