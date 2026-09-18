package com.asteria.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA 路由回退：替代 nginx 的 try_files。
 * static/ 下找不到真实文件时，前端路由（无扩展名、非 api）一律返回 index.html，
 * 由 Vue Router 在浏览器端接管渲染。
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // API 路径绝不回退给 index.html，保持 404 语义
                        if (path.startsWith("api/")) {
                            return null;
                        }
                        // 带扩展名的静态资源找不到就是 404，不给 HTML
                        if (path.contains(".")) {
                            return null;
                        }
                        // 剩下的都是前端路由（/chat、/banks/3…）→ 交给 SPA
                        return new ClassPathResource("static/index.html");
                    }
                });
    }
}
