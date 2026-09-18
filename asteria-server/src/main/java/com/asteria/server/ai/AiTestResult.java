package com.asteria.server.ai;

/**
 * AI 连通性测试结果。
 *
 * <p>record 会被 Jackson 直接序列化成 {"ok":true,"message":"连接正常"}，
 * 所以接口的 data 直接返回它 —— 前端既能用 ok 判断成败，又能把 message 显示给用户。
 *
 * @param ok      是否连通
 * @param message 原因：成功时是"连接正常"，失败时说明具体原因（如"API Key 无效（HTTP 401）"）
 */
public record AiTestResult(boolean ok, String message) {
}
