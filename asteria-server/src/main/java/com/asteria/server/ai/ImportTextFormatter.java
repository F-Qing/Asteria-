package com.asteria.server.ai;

import com.asteria.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 上传时的「AI 格式化」组件：把从网页/Word 复制来的杂乱文本，整理成 QuestionParser 认的标准格式。
 *
 * <p>它是兜底手段，不是默认路径 —— 只有文件按原样解析不出题目时才会被调用（见 BanksServiceImpl）。
 *
 * <p>为什么要分块：模型单次输出有上限，长文档一次性整理会被截断，而且截断是"静默丢题"
 * （不报错、就是少几道题），最难发现。分块还能顺便给前端报进度。
 */
@Component
@Slf4j
public class ImportTextFormatter {

    /** 每块最多多少字：切小一点更安全（不容易被截断），但调用次数和总耗时更多 */
    private static final int CHUNK_CHARS = 6000;

    /** 硬上限：万一整篇没有空行，也得强制切开，防止块无限大把上下文撑爆 */
    private static final int CHUNK_HARD_LIMIT = CHUNK_CHARS * 2;

    /** 整理只允许"调结构"，绝不允许发挥 → 温度压到最低 */
    private static final double TEMPERATURE = 0.1;

    /**
     * 整理规则。和前端「题目格式要求」弹窗里那份保持一致，
     * 但去掉了"把结果保存成 txt 文件"那条 —— 后端要的是纯文本返回值，不是文件。
     */
    private static final String SYSTEM_PROMPT = """
            请按下面的标准格式整理题目文本。

            【重要】只调整结构和补充标签：不得改动任何原文文字，不得编造或补全答案，不得增删题目。
            【重要】你收到的可能是一个长文档的片段：只整理你收到的内容，不要补全上文，不要假设前面还有题。

            标准格式：
            【第 1 题】题型：单选题
            题目：题干内容
            选项：
              A. 选项内容
              B. 选项内容
              C. 选项内容
              D. 选项内容
            正确答案：A

            格式要求：
            1. 每道题以【第 N 题】开头，N 从 1 开始递增
            2. 题型只能是这五种之一：单选题、多选题、判断题、填空题、简答题
            3. 选项行固定写成「字母. 内容」；判断题也要写成 A. 正确 / B. 错误
            4. 答案写法：
               - 单选题：单个字母，如 A
               - 多选题：字母连写、不分隔、升序，如 ACD
               - 判断题：A 表示正确，B 表示错误
               - 填空题：多个空用中文分号「；」分隔
               - 简答题：答案原文
            5. 原文没给答案的，「正确答案：」后面留空，不要自己编
            6. 只输出整理后的纯文本，不要任何说明文字，不要 markdown 代码块
            """;

    @Autowired
    private AiChatModelFactory chatModelFactory;

    /**
     * 把杂乱文本整理成标准格式。
     *
     * @param rawText    原始纯文本
     * @param config     用户的 AI 配置（key / baseUrl / model）
     * @param onProgress 进度回调（0~100），可为 null
     * @return 整理后的文本（各块直接拼接）
     */
    public String format(String rawText, AiRequestConfig config, IntConsumer onProgress) {
        List<String> chunks = splitIntoChunks(rawText);
        log.info("AI 整理格式开始：原文 {} 字，切成 {} 块", rawText.length(), chunks.size());

        StringBuilder result = new StringBuilder(rawText.length());
        for (int i = 0; i < chunks.size(); i++) {
            result.append(formatOneChunk(chunks.get(i), config)).append('\n');
            log.info("AI 整理格式进度：{}/{}", i + 1, chunks.size());
            if (onProgress != null) {
                onProgress.accept((i + 1) * 100 / chunks.size());
            }
        }
        return result.toString();
    }

    /** 整理一块；失败直接抛 —— 半整理的结果不可信，宁可整批失败让人重试 */
    private String formatOneChunk(String chunk, AiRequestConfig config) {
        OpenAiChatModel chatModel = chatModelFactory.create(config, TEMPERATURE);
        try {
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage("待整理文本：\n" + chunk))));
            String text = response.getResult().getOutput().getText();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("AI 返回了空内容");
            }
            return text.trim();
        } catch (Exception e) {
            // 上游异常原文可能带 key，统一脱敏后再往外给
            throw new BusinessException(50001, "AI 整理文件失败：" + AiErrors.mask(e, config));
        }
    }

    /**
     * 按字数切块，切点优先选【空行】。
     *
     * <p>题与题之间通常有空行，在那里切最不容易把一道题劈成两半
     * （题干在前一块末尾、选项在后一块开头 → 变成两道残题）。
     */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\n")) {
            current.append(line).append('\n');
            boolean reachedSize = current.length() >= CHUNK_CHARS && line.isBlank();
            if (reachedSize || current.length() >= CHUNK_HARD_LIMIT) {
                chunks.add(current.toString());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }
}
