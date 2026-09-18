package com.asteria.server.ai;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * 按用户的配置（请求头带来的 key / baseUrl / model）现场造一个 OpenAiChatModel。
 *
 * <p>为什么不做成启动时的单例 bean：BYOK 模式下 key 和 baseUrl 每个请求都可能不同，
 * 复用同一个会把 A 用户的 key 用到 B 用户的请求上。这里每请求现造，用完即弃。
 *
 * <p>工厂本身无状态，所以它是单例；产品（ChatModel）每次都是新的。
 */
@Component
public class AiChatModelFactory {

    /**
     * @param temperature 温度由【调用方按功能】决定：测试连通性传 null（跟服务商默认），
     *                    题目解析传 0.3（要稳定），以后聊天传 0.7（要自然）。null = 不设置该参数。
     */
    public OpenAiChatModel create(AiRequestConfig config, Double temperature) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.endpoint(""))         // 顺手去掉用户可能多填的结尾斜杠
                .apiKey(config.apiKey())              // ★ key 只用在这一处
                .completionsPath("/chat/completions") // 前端 baseUrl 已含版本段，别再补 /v1
                .build();

        // 关掉 Spring AI 默认的"重试 10 次 + 指数退避"：
        // 批量解析是一道一道跑，宁可快速失败、由外层记账，也不要一道题卡几分钟
        RetryTemplate noRetry = RetryTemplate.builder().maxAttempts(1).build();

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder().model(config.model());
        if (temperature != null) {
            options.temperature(temperature);
        }

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options.build())
                .retryTemplate(noRetry)
                .build();
    }
}
