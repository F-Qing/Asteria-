package com.asteria.server.Services;

import com.asteria.pojo.entity.ChatMessage;

import com.asteria.pojo.entity.AiChatBody;
import com.asteria.pojo.entity.DTO.ChatSessionCreateDTO;
import com.asteria.pojo.entity.VO.ChatSessionVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import com.asteria.server.ai.AiRequestConfig;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 聊天 Service。
 *
 * <p>和 AiService 的分工：AiService 管"配置测试 / 知识点总结"，这里管"会话与消息"。
 *
 * <p>注意本接口只接收"配置对象"：<b>签名里不出现 HttpServletRequest</b>，
 * 从请求头取 AI 配置是 Web 层（Controller + AiHeaders）的活。
 */
public interface ChatService {

    /**
     * 新建会话。
     *
     * @param dto 可为 null（前端固定发 {}，但 curl 测试时可能不带 body）
     * @return 新建出来的会话（含数据库回填的 id 和创建时间）
     */
    ChatSessionVO createSession(ChatSessionCreateDTO dto);

    List<ChatSessionVO> getSessions();

    void deleteSession(Long id);

    /**
     * 查会话的历史消息。
     *
     * <p><b>正序</b>（最早的在前）：前端拿到 list 直接渲染、新消息往末尾追加，
     * 顺序反了刷新一次对话就上下颠倒了。
     *
     * <p>会话不存在时抛 40404，不返回空列表 —— 否则前端分不清"没有消息"
     * 和"这个会话已经被删了"。
     *
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数（默认 50，最大 200）
     */
    PageResultVO<ChatMessage> getMessages(Long id, Integer page, Integer pageSize);

    Flux<String> sendMessage(Long id, AiChatBody body,AiRequestConfig aiConfig);

    ChatMessage saveMessage(Long id, String role, String full, boolean isError);
}