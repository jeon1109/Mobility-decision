package com.example.musinsaPointSystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * React SPA 연동 설정.
 * - 개발: Vite(5173) + CORS
 * - 운영: static/index.html 빌드 산출물 + SPA fallback
 */
@Configuration
public class ReactWebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173", " http://64.110.109.47")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    /**
     * API·Swagger가 아닌 GET 요청은 React index.html 로 fallback.
     */
    private static class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            if (isApiPath(resourcePath)) {
                return null;
            }

            Resource requested = super.getResource(resourcePath, location);
            if (requested != null && requested.exists()) {
                return requested;
            }

            Resource index = new ClassPathResource("/static/index.html");
            return index.exists() ? index : null;
        }

        private boolean isApiPath(String path) {
            return path.startsWith("api/")
                    || path.startsWith("v1/")
                    || path.startsWith("swagger-ui")
                    || path.startsWith("v3/api-docs")
                    || path.startsWith("actuator/");
        }
    }
}
