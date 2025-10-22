package com.ruoyi.license.biz;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SignatureInterceptor signatureInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signatureInterceptor)
                // 拦截需要签名验证的接口
                .addPathPatterns("/api/v2/activate", "/api/v2/heartbeat")
                // 排除健康检查等不需要签名的接口
                .excludePathPatterns("/api/v2/health", "/api/admin/**");
    }
}