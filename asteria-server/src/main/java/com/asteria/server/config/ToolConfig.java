package com.asteria.server.config;

import com.asteria.common.Tool.FileTextReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用工具类 Bean 注册。
 */
@Configuration
public class ToolConfig {

    @Bean
    public FileTextReader fileTextReader() {
        return new FileTextReader();
    }

}