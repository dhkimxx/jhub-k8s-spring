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

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jhub.k8s")
public class JhubK8sProperties {

    @NotBlank
    private String namespace;

    @NotBlank
    private String usernameLabelKey;

    @NotBlank
    private String podNamePrefix;

    private boolean useKubeconfig = false;

    private String apiServerUrl;

    private String bearerToken;

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
