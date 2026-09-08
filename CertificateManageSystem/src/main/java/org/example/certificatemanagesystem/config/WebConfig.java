package org.example.certificatemanagesystem.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    // 上传根目录外置到 application.yml（upload.base-path），换机器/部署无需改代码
    @Value("${upload.base-path}")
    private String uploadBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadBasePath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 统一登录校验：拦截器内部维护公开路径白名单，新增接口默认要求登录
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }
}
