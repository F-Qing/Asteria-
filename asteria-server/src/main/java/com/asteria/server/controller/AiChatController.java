package com.asteria.server.controller;

import com.asteria.common.result.ApiResponse;
import com.asteria.pojo.entity.AiChatBody;
import com.asteria.pojo.entity.ChatMessage;
import com.asteria.pojo.entity.DTO.ChatSessionCreateDTO;
import com.asteria.pojo.entity.VO.ChatSessionVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import com.asteria.server.Services.ChatService;
import com.asteria.server.ai.AiErrors;
import com.asteria.server.ai.AiHeaders;
import com.asteria.server.ai.AiRequestConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class AiChatController {

    /**
     * SSE 超时时间：0 = 不超时。
     * AI 生成几十秒很正常，用默认的 30 秒会把长回复从中间掐断。
     */
    private static final long SSE_TIMEOUT_MS = 0L;

    private final ChatService chatService;

    public AiChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    /**
     * 新建会话。Body 可以为空（前端固定发 {}，curl 测试时也方便）。
     *
     * <p>注意：这个接口<b>不需要 AI 配置</b> —— 建会话跟调模型没关系，
     * 所以这里没有 AiHeaders；只有"发消息"那个接口才需要，别让没配 AI 的用户连会话都建不了。
     */
    @PostMapping("/sessions")
    public ApiResponse<ChatSessionVO> createSession(@RequestBody(required = false) ChatSessionCreateDTO dto) {
        return ApiResponse.ok(chatService.createSession(dto));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return ApiResponse.ok();
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionVO>> getSessions() {
        return ApiResponse.ok(chatService.getSessions());
    }

    /**
     * 历史消息：GET /api/chat/sessions/{id}/messages?page=1&amp;pageSize=50
     *
     * <p><b>不需要 AiHeaders</b>：看历史跟调模型无关。要是在这里 require AI 配置，
     * 用户换个浏览器（key 存在本地）连自己的历史都看不了。
     */
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<PageResultVO<ChatMessage>> getMessages(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize) {
        return ApiResponse.ok(chatService.getMessages(id, page, pageSize));
    }

    /**
     * 发送消息（SSE 流式）。
     *
     * <p><b>为什么返回类型是 SseEmitter 而不是 Flux&lt;String&gt;</b>：
     * 前端 utils/sse.ts 约定的事件格式是「event: chunk/done/error + JSON 格式的 data」。
     * 直接返回 Flux 时，Spring 只会把每个元素包成一行 `data:裸文本`（没有 event 名、
     * 内容也不是 JSON），前端三个分支一个都匹配不上，会全部丢弃 —— 表现就是"发了没回复"。
     */
    @PostMapping(value = "/sessions/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long id, @RequestBody AiChatBody body,
                                  HttpServletRequest request) {
        AiRequestConfig aiConfig = AiHeaders.require(request);
        log.info("发送消息：sessionId={}, content={}", id, body.getContent());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        // 累积完整回复：done 事件要用；落库也要用
        // 注意：用户消息的落库在 ChatService.sendMessage 里（Service 要先取历史再落库，
        // 否则本轮提问会被算进历史、发给模型两次），这里只负责 AI 这一侧
        StringBuilder full = new StringBuilder();
        chatService.sendMessage(id, body, aiConfig).subscribe(
                // ① 每收到一块 → 推一个 chunk 事件（打字机效果靠它）
                delta -> {
                    full.append(delta);
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(Map.of("delta", delta)));
                    } catch (IOException e) {
                        // 客户端断开（用户点"停止" / 关了页面）：别再推了
                        log.warn("SSE 客户端已断开：sessionId={}", id);
                        emitter.completeWithError(e);
                    }
                },
                // ② 出错 → 推一个 error 事件（错误信息先脱敏，可能带 key）
                error -> {
                    if (!full.isEmpty()) {
                        chatService.saveMessage(id, "assistant", full.toString(), true);
                    }
                    Exception ex = error instanceof Exception e ? e : new Exception(error);
                    String reason = AiErrors.mask(ex, aiConfig);
                    log.warn("AI 回复失败：sessionId={}, 原因={}", id, reason);
                    try {
                        emitter.send(SseEmitter.event().name("error")
                                .data(Map.of("code", 50000, "message", reason)));
                    } catch (IOException ignored) {
                        // 客户端也断了，没法再通知
                    }
                    emitter.complete();
                },
                // ③ 正常结束 → 推 done 事件（前端靠它把 streaming 置回 false，解锁输入框）
                () -> {
                    ChatMessage saved = chatService.saveMessage(id, "assistant", full.toString(), false);
                    Map<String, Object> done = new LinkedHashMap<>();
                    done.put("messageId", saved.getId());
                    done.put("content", full.toString());
                    try {
                        emitter.send(SseEmitter.event().name("done").data(done));
                    } catch (IOException ignored) {
                    }
                    emitter.complete();
                    log.info("AI 回复结束：sessionId={}, 回复长度={}", id, full.length());
                });

        return emitter;
    }
}
