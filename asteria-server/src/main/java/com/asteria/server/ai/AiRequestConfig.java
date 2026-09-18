package com.asteria.server.ai;

/**
 * 用户带来的 AI 配置快照。
 *
 * <p>只活在一次请求里：从请求头取出来 → 用它调 AI → 请求结束就没了。
 * 不落库、不进日志、不常驻内存。
 *
 * <p>为什么用 record（而不是 @Data 普通类）：
 * 字段是 final、不可变 —— 这份配置可能会从 Tomcat 请求线程传进导入后台线程，
 * 不可变 = 谁都不能改 = 天然线程安全。
 *
 * <p>为什么没有 temperature / maxTokens：
 * 它们是"按任务"定的工程参数（生成解析要 0.3 求稳定、闲聊要 0.7），不是用户偏好，
 * 所以由后端在各自的功能里写死，不开放给设置页。
 */
public record AiRequestConfig(String provider, String apiKey, String baseUrl, String model) {

    /**
     * 调用 AI 需要的最小信息齐了吗：key + 模型 + 地址。
     *
     * <p>注意它只能判断"填没填"，判断不了"填得对不对" ——
     * key 是否有效只有真的请求一次 AI 服务商才知道。
     */
    public boolean usable() {
        return notBlank(apiKey) && notBlank(model) && notBlank(baseUrl);
    }

    /**
     * 拼出某个接口的完整地址：endpoint("/models") → "https://api.deepseek.com/models"
     *
     * <p>顺手处理用户把 baseUrl 写成带结尾斜杠的情况
     * （"https://api.deepseek.com/" 不会拼成 "...//models"）。
     */
    public String endpoint(String path) {
        if (isBlank(baseUrl)) {
            return "";
        }
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    /**
     * 给密钥打码：万一手滑写了 log.info("{}", config)，也不会把明文 key 打进日志。
     * 这是最后一道保险 —— 正常做法仍然是"打日志时根本不要碰这个对象"。
     */
    @Override
    public String toString() {
        return "AiRequestConfig[provider=%s, model=%s, baseUrl=%s, apiKey=%s]"
                .formatted(provider, model, baseUrl, apiKey == null ? "null" : "***");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
