package com.asteria.pojo.entity.DTO;

import lombok.Data;

/**
 * 新建会话的请求体。
 *
 * <p>只有两个字段：会话<b>不绑定题库/章节</b> —— 需要题库内容时由 AI 通过"工具调用"自己去查。
 *
 * <p>两个字段都可空：title 为空表示"用首条消息自动生成"，agentMode 缺省按 BUILTIN 处理。
 */
@Data
public class ChatSessionCreateDTO {

    /** 会话标题，缺省为空串 */
    private String title;

    /** 模式：BUILTIN / EXTERNAL，缺省 BUILTIN */
    private String agentMode;
}
