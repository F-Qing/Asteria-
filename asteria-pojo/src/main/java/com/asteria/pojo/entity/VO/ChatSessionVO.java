package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 聊天会话 VO —— 对外返回的会话结构。
 *
 * <p>字段严格照前端契约（types/index.ts 的 ChatSession）和接口文档，<b>只有 5 个</b>：
 * 表里虽然有 created_at，但对外不暴露，所以这里没有 createdAt。
 *
 * <p>和实体 ChatSession 的分工：实体是"表长什么样"，VO 是"接口对外长什么样"，
 * 两者故意不同步，改任一边不会牵扯另一边。
 */
@Data
public class ChatSessionVO {

    /** 会话 ID */
    private Long id;

    /** 标题（首条消息后自动生成，用户可改；可能为空串） */
    private String title;

    /** 模式：BUILTIN 内置模型 / EXTERNAL 外部 */
    private String agentMode;

    /** 消息条数（用户+AI 都算） */
    private Integer messageCount;

    /** 最近活动时间（会话列表按它倒序） */
    private LocalDateTime updatedAt;
}
