package com.dhkimxx.jhub_k8s_spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI (Swagger) 설정 클래스.
 * - API 문서화 메타데이터 제공
 * - Swagger UI 리소스 핸들러 매핑
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JHub K8s Spring API")
                        .description("API for monitoring and controlling JupyterHub on Kubernetes")
                        .version("v0.0.1"));
    }

    // Swagger UI 리소스 명시적 매핑
    // Spring Boot 설정에 따라 자동 매핑이 실패하는 경우를 대비해 안전하게 리소스를 등록합니다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/4.15.5/");
    }
}
