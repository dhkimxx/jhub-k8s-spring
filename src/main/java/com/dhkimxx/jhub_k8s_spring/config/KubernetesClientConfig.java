package com.dhkimxx.jhub_k8s_spring.config;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.EventsV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

/**
 * 쿠버네티스 클라이언트(ApiClient) 및 API 인스턴스 빈 설정.
 * Kubeconfig 파일 또는 직접 설정을 통해 클라이언트를 초기화합니다.
 */
@Configuration
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JhubK8sProperties.class)
public class KubernetesClientConfig {

    /**
     * 기본 Kubernetes ApiClient 빈 생성.
     * 설정에 따라 Kubeconfig 파일 로드 또는 직접 설정을 수행합니다.
     */
    @Bean
    public ApiClient apiClient(JhubK8sProperties properties) throws IOException {
        ApiClient client = shouldUseKubeconfig(properties)
                ? ClientBuilder.kubeconfig(loadKubeConfig(properties)).build()
                : buildDirectClient(properties);

        Duration timeout = properties.getRequestTimeout();
        client.setConnectTimeout((int) timeout.toMillis());
        client.setReadTimeout((int) timeout.toMillis());
        return client;
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public EventsV1Api eventsV1Api(ApiClient apiClient) {
        return new EventsV1Api(apiClient);
    }

    @Bean
    public CustomObjectsApi customObjectsApi(ApiClient apiClient) {
        return new CustomObjectsApi(apiClient);
    }

    private KubeConfig loadKubeConfig(JhubK8sProperties properties) throws IOException {
        try (FileReader reader = new FileReader(properties.getKubeconfigPath())) {
            return KubeConfig.loadKubeConfig(reader);
        }
    }

    private ApiClient buildDirectClient(JhubK8sProperties properties) throws IOException {
        if (!StringUtils.hasText(properties.getApiServerUrl())) {
            throw new IllegalStateException("API server URL must be configured when kubeconfig is disabled.");
        }
        if (!StringUtils.hasText(properties.getBearerToken())) {
            throw new IllegalStateException("Bearer token must be configured when kubeconfig is disabled.");
        }

        ApiClient client = ClientBuilder.standard()
                .setBasePath(properties.getApiServerUrl())
                .setVerifyingSsl(properties.isVerifySsl())
                .build();

        client.setApiKeyPrefix("Bearer");
        client.setApiKey(properties.getBearerToken());
        return client;
    }

    private boolean shouldUseKubeconfig(JhubK8sProperties properties) {
        if (properties.isUseKubeconfig()) {
            return true;
        }
        boolean hasDirectConfig = StringUtils.hasText(properties.getApiServerUrl())
                && StringUtils.hasText(properties.getBearerToken());
        if (hasDirectConfig) {
            return false;
        }
        String serviceHost = System.getenv("KUBERNETES_SERVICE_HOST");
        String servicePort = System.getenv("KUBERNETES_SERVICE_PORT");
        return serviceHost == null || serviceHost.isBlank() || servicePort == null || servicePort.isBlank();
    }
}
