package com.asteria.server.Services;

import com.asteria.common.result.ApiResponse;
import com.asteria.pojo.entity.DTO.AiSummaryDTO;
import com.asteria.pojo.entity.VO.AiSummaryVo;
import com.asteria.server.ai.AiRequestConfig;
import com.asteria.server.ai.AiTestResult;

/**
 * AI 能力 Service。
 *
 * <p>目前只有"测试连通性"；以后会话相关的（聊天 / 流式 / 给题目生成解析 / 知识点总结）
 * 都先往这里加，接口超过 5~6 个、能明显分成两类了，再拆成
 * AiChatService、AiAnalysisService —— 不提前拆。
 *
 * <p><b>注意本接口只接收"配置对象"，签名里不出现 HttpServletRequest</b>：
 * "从 HTTP 请求里取配置"是 Web 层（Controller + AiHeaders）的活。
 * 这样以后从别的入口调用（导入后台线程、定时任务）也不用改 Service，
 * 换实现（比如改用官方 SDK）也只动 AiServiceImpl。
 */
public interface AiService {

    /**
     * 测试这份 AI 配置能不能用（会真的请求一次 AI 服务商）。
     *
     * <p>刻意不抛异常：失败也返回结果对象，由调用方决定怎么展示。
     *
     * @param config 用户配置（key / model / baseUrl）；传 null 或配置不完整时直接返回失败结果
     * @return ok 是否连通，message 失败原因（可直接展示给用户）
     */
    AiTestResult testConnection(AiRequestConfig config);

    ApiResponse<AiSummaryVo> createSummary(AiSummaryDTO dto, AiRequestConfig aiConfig);

    /**
     * 查询题库已生成的知识点总结（**只读缓存，不会调 AI**，所以不需要 AI 配置）。
     *
     * @param bankId 题库 id
     * @return 生成过就返回总结（fromCache=true），从没生成过返回 null
     */
    AiSummaryVo getSummary(Long bankId);
}
