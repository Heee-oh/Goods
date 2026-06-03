package com.goods.market.common.config;

import com.goods.market.common.interceptor.RegionVerificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path baseDir;
    private final RegionVerificationInterceptor regionVerificationInterceptor;

    public WebMvcConfig(@Value("${app.storage.base-dir:uploads}") String baseDir, RegionVerificationInterceptor regionVerificationInterceptor) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
        this.regionVerificationInterceptor = regionVerificationInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(baseDir.toUri().toString());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(regionVerificationInterceptor)
                .addPathPatterns("/api/listings")
                .addPathPatterns("/api/listings/**");
    }
}
