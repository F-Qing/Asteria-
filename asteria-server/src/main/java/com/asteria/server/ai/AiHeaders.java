package com.asteria.server.ai;

import com.asteria.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 从请求头里取出用户的 AI 配置（BYOK）—— Web 层的取值工具。
 *
 * <p>前端在设置页填的配置存在浏览器 localStorage，每次请求由 axios 拦截器挂到
 * X-AI-* 请求头上带过来；后端不存它，每次现取现用，用完即弃。
 *
 * <p>这里就是"抽出来的工具方法"：所有需要 AI 的接口都调它，取值逻辑只写一遍。
 * 它只是"把请求翻译成配置对象"，不含任何业务判断。
 */
public final class AiHeaders {

    /** 工具类不需要被实例化 */
    private AiHeaders() {
    }

    /**
     * 取出配置；缺关键项时返回 <b>null</b>（不抛异常）。
     *
     * <p>给"没有 AI 也能继续跑"的地方用，比如导入题库：
     * 用户没配 AI 时应该跳过 AI 解析、正常入库。
     */
    public static AiRequestConfig from(HttpServletRequest request) {
        String apiKey = request.getHeader("X-AI-Key");
        String model = request.getHeader("X-AI-Model");
        String baseUrl = request.getHeader("X-AI-Base-Url");
        if (isBlank(apiKey) || isBlank(model) || isBlank(baseUrl)) {
            return null;
        }
        return new AiRequestConfig(
                request.getHeader("X-AI-Provider"),
                apiKey,
                baseUrl,
                model);
    }

    /**
     * 取出配置；缺关键项直接抛 40020。
     *
     * <p>给"没有 AI 就用不了"的地方用，比如 AI 聊天接口。
     */
    public static AiRequestConfig require(HttpServletRequest request) {
        AiRequestConfig config = from(request);
        if (config == null) {
            throw new BusinessException(40020, "请先在「设置」页配置 AI 服务（API Key / 模型 / Base URL）");
        }
        return config;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
