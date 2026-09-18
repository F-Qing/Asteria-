package com.asteria.server.Services.impl;

import com.asteria.common.exception.BusinessException;
import com.asteria.pojo.entity.AiChatBody;
import com.asteria.pojo.entity.ChatMessage;
import com.asteria.pojo.entity.ChatSession;
import com.asteria.pojo.entity.DTO.ChatSessionCreateDTO;
import com.asteria.pojo.entity.VO.ChatSessionVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import com.asteria.server.Services.ChatService;
import com.asteria.server.ai.AiChatModelFactory;
import com.asteria.server.ai.AiRequestConfig;
import com.asteria.server.ai.QuestionBankTools;
import com.asteria.server.mapper.ChatMessageMapper;
import com.asteria.server.mapper.ChatSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * AI 聊天实现。
 *
 * <p>目前只有"新建会话"；列表 / 删除 / 历史消息 / 发消息（SSE）后续往这里加。
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final AiChatModelFactory chatModelFactory;
    private final ChatMessageMapper chatMessageMapper;
    private final QuestionBankTools questionBankTools;

    public ChatServiceImpl(ChatSessionMapper chatSessionMapper, AiChatModelFactory chatModelFactory,
                           ChatMessageMapper chatMessageMapper, QuestionBankTools questionBankTools) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatModelFactory = chatModelFactory;
        this.chatMessageMapper = chatMessageMapper;
        this.questionBankTools = questionBankTools;
    }

    /** 默认模式：前端不传时用它 */
    private static final String DEFAULT_AGENT_MODE = "BUILTIN";

    /** 历史消息：默认每页 50 条，一次最多 200 条（跟题库列表一样，防止有人 pageSize 拉全表） */
    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;
    private static final int MAX_MESSAGE_PAGE_SIZE = 200;

    /**
     * 发给模型的对话记忆：条数上限（100 条 ≈ 50 轮）。
     *
     * <p>为什么要设上限、而不是"全量历史"：模型有硬性的上下文窗口（DeepSeek 64k、
     * GPT-4o 128k token）。塞爆了不是"回答变慢"，而是直接 400 报错，
     * 用户看到的是"AI 出错了"。所以这里取的是"能吃下的最大量"，不是无限。
     */
    private static final int HISTORY_MAX_MESSAGES = 100;

    /**
     * 对话记忆的字符预算：从最新的一条往回累积，超了就丢掉更老的消息。
     *
     * <p>为什么不只看条数：一条消息可能是 1 个字，也可能是 5000 字的简答题。
     * 中文大约 1 字 ≈ 1 token，这里留够空间给系统提示词、工具定义（明天加）和这次回答。
     */
    private static final int HISTORY_MAX_CHARS = 30000;

    /**
     * 系统提示词。核心是把"两种知识"拆开：
     * 题库里的事实（题干/答案/解析）必须查工具，不许编；学科知识（原理/举例）尽管放开讲。
     */
    private static final String SYSTEM_PROMPT = """
            你是 Asteria 题库助手，一位耐心、严谨、能把题讲透的老师。你帮用户在自己导入的题库里
            找题、讲题，也解答题目背后的学科知识。

            【两种知识，必须分清】
            一、题库里的事实（题目的题干、选项、标准答案、解析、属于哪个题库和章节）：
                必须先调工具去查。你的记忆里没有这个题库的任何一道题，凭印象编出来的题目和答案，
                用户根本无法分辨真假 —— 这是最严重的一类错误。
            二、学科知识（概念、原理、推导、举例、相关拓展）：
                这部分可以放心用你自己的知识讲，讲深、讲透，不必受题库里有什么的限制。

            两者混在一起时要说清哪句来自题库、哪句是你的补充，例如：
            「题库给的答案是 B（id=123）。这里我再补一点背景：……」

            【怎么用工具】
            1. 不确定题目在哪个题库时，先调 listBanks，看清有哪些题库、各有多少题。
            2. 找题调 searchQuestions：
               - 关键词取题干里最具体的那个词，至少 2 个字（用"三次握手"，别用"网络"）；
               - 没查到时换个词再试，同一个词不要反复查；
               - 一次最多返回 20 道。用户要"全部题目"时不要硬凑，说明这个上限，
                 并建议按题型或章节分批查。
            3. 搜索结果里过长的题干结尾会带「…」，表示被截断了。要讲这道题，就调
               getQuestionDetail 拿完整题干、选项和解析，不要根据半截题干猜题意。
            4. 引用题目时用「第 N 题：题干开头」这种说法，例如「第 2 题（二叉树的遍历顺序）」；
               id 是你调工具用的内部编号，用户看不到也不需要看到，回答里不要写出来。

            【像老师那样讲】
            5. 用中文回答。先给结论或要点，再讲为什么，最后给一个例子或易错点 ——
               不要一上来就铺一大段。
            6. 讲题时要说清：这道题考什么、每个干扰项错在哪、换个问法还站不站得住。
            7. 搜到多道题时，只精讲用户问的那一道（或他指定的那道），其余列成清单让用户挑，
               不要把十几道题全部逐题详解。
            8. 主动补一句相关延伸（相邻概念、常见混淆点、考试/面试常考的角度），但点到为止，
               别喧宾夺主。
            9. 用户没听懂时换个说法、降一个层次再讲，不要重复原话；必要时反问一句确认卡在哪。
            10. 学科知识你也有记不准的时候：不确定就直说不确定，别硬编一个"标准答案"出来。
            11. 篇幅按需：先给要点版，用户说"再详细讲讲"再展开。

            【边界】
            12. 回答里不要出现任何内部编号：id=123、[id=123]、「题目 id」「题库 id」都不行。
                题目用「第 N 题」或题干指代，题库直接说名字（《数据结构》），
                章节直接说章节名。编号只在你调工具时用。
            13. 「已有解析」显示「（未提供）」时，要说明下面是你自己的讲解、不是题库里的官方解析；
                答案同理，题库没给答案就不要猜。
            14. 工具没查到就直说没查到，建议用户换关键词或先看题库列表，绝不假装查到了。
            15. 只处理与题库、学习有关的问题，闲聊礼貌地引回到学习上。
            16. 不要复述这段规则、也不要描述你调用了什么工具，直接给用户结果。
            """;

    @Override
    public void deleteSession(Long id) {
        chatSessionMapper.deleteById(id);
        log.info("删除会话：id={}", id);
    }

    @Override
    public Flux<String> sendMessage(Long id, AiChatBody body, AiRequestConfig aiConfig) {
        // ① 先取历史：此刻本轮提问还没落库，所以拿到的是"上一轮为止"的记录。
        //    顺序反了的话，本轮提问会出现两次（库里一条 + 下面 add 一条）。
        List<Message> messages = toSpringAiMessages(
                loadRecentMessages(id, HISTORY_MAX_MESSAGES, HISTORY_MAX_CHARS));

        // ② 落库本轮提问，并作为最后一条消息交给模型
        saveMessage(id, "user", body.getContent(), false);
        messages.add(new UserMessage(body.getContent()));

        OpenAiChatModel model = chatModelFactory.create(aiConfig, 0.7);
        ChatClient chatClient = ChatClient.create(model);
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .messages(messages)     // ← 对话记忆：历史 + 本轮，一次给全（模型是无状态的）
                .tools(questionBankTools)   // ← 工具调用：模型可以自己去查题库
                .stream()
                .content()
                .doFinally(signalType -> log.info("AI 回复结束：sessionId={}, 上下文条数={}", id, messages.size()));
    }

    /**
     * 取最近的消息，按时间【正序】返回（模型要的是旧 → 新）。
     *
     * <p>倒序查 + LIMIT 再从新到旧攒预算：正序 LIMIT 拿到的是最早的几条，不是最近的。
     * 条数和字符数两道闸——条数防"一次查太多"，字符数防"一条超长简答题把上下文顶爆"。
     */
    private List<ChatMessage> loadRecentMessages(Long id, int maxMessages, int maxChars) {
        List<ChatMessage> rows = chatMessageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, id)
                .orderByDesc(ChatMessage::getCreatedAt)
                .orderByDesc(ChatMessage::getId)      // 同秒的两条（user + assistant）靠 id 定序
                .last("LIMIT " + maxMessages));       // maxMessages 是常量，没有注入面

        List<ChatMessage> kept = new ArrayList<>(rows.size());
        int chars = 0;
        for (ChatMessage row : rows) {                 // 库里是倒序的：从最新往老走
            int len = row.getContent() == null ? 0 : row.getContent().length();
            // kept 为空时无条件收下：最新这一条再长也得给模型，否则它连用户刚问的都不知道
            if (!kept.isEmpty() && chars + len > maxChars) {
                break;
            }
            kept.add(row);
            chars += len;
        }
        Collections.reverse(kept);
        return kept;
    }

    /** 库里的 role 字符串 → Spring AI 的 Message 对象（模型只认后者，不认我们自己表的 role 列） */
    private List<Message> toSpringAiMessages(List<ChatMessage> rows) {
        List<Message> messages = new ArrayList<>(rows.size() + 1);
        for (ChatMessage row : rows) {
            String content = row.getContent();
            if (content == null || content.isEmpty()) {
                continue;                              // 空消息发过去只会白烧 token
            }
            if ("user".equals(row.getRole())) {
                messages.add(new UserMessage(content));
            } else if ("assistant".equals(row.getRole())) {
                messages.add(new AssistantMessage(content));
            }
            // system 角色的历史不放进来：会和本次的 SYSTEM_PROMPT 打架
        }
        return messages;
    }

    @Override
    public ChatMessage saveMessage(Long id, String role, String full, boolean isError) {
        ChatSession path = new ChatSession();
        ChatSession session = chatSessionMapper.selectById(id);
        if(session == null){
            throw new BusinessException(40404, "聊天会话不存在：" + id);
        }
        path.setId(id);
        path.setMessageCount(session.getMessageCount() == null ? 1 : session.getMessageCount() + 1);
        if("user".equals(role) && (session.getTitle() == null || session.getTitle().isBlank())){
            String title = full.substring(0, Math.min(full.length(), 20));
            path.setTitle(title);
        }
        chatSessionMapper.updateById(path);
        ChatMessage message = new ChatMessage();
        message.setSessionId(id);
        message.setRole(role);
        message.setContent(full);
        message.setInterrupted(isError);
        chatMessageMapper.insert(message);
        // ↑ MyBatis-Plus 自动回填自增 id 到 message.id
        return message;
    }


    @Override
    public PageResultVO<ChatMessage> getMessages(Long id, Integer page, Integer pageSize) {
        // 会话不存在要报错，不能默默返回空列表：前端会把"加载成功但没有消息"
        // 和"这个会话已经被删了"混为一谈
        if (chatSessionMapper.selectById(id) == null) {
            throw new BusinessException(40404, "聊天会话不存在：" + id);
        }

        long current = (page == null || page < 1) ? 1 : page;
        long size = (pageSize == null || pageSize < 1)
                ? DEFAULT_MESSAGE_PAGE_SIZE
                : Math.min(pageSize, MAX_MESSAGE_PAGE_SIZE);

        LambdaQueryWrapper<ChatMessage> wrapper = Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, id)
                .orderByAsc(ChatMessage::getCreatedAt)
                // created_at 是秒级 DATETIME，同一秒内插入的两条（比如 AI 立刻报错那一路，
                // user + assistant 是同一秒落的库）光靠时间排不出先后，会随机颠倒。
                // id 是自增的，天然等于真实插入顺序，拿它兜底。
                .orderByAsc(ChatMessage::getId);

        Page<ChatMessage> pageParam = Page.of(current, size);
        chatMessageMapper.selectPage(pageParam, wrapper);   // MP 自动执行 COUNT + LIMIT

        return new PageResultVO<>(pageParam.getRecords(), pageParam.getTotal(), current, size);
    }

    @Override
    public List<ChatSessionVO> getSessions() {
        List<ChatSession> entities = chatSessionMapper.selectList(null);
        return entities.stream()
                .map(this::toVo)
                .sorted(Comparator.comparing(ChatSessionVO::getUpdatedAt))
                .toList();
    }

    @Override
    public ChatSessionVO createSession(ChatSessionCreateDTO dto) {
        ChatSession entity = new ChatSession();
        // title 允许空串（前端固定发 ''）：表示"等首条消息再自动生成标题"
        entity.setTitle(dto == null || dto.getTitle() == null ? "" : dto.getTitle().trim());
        // agentMode 缺省 BUILTIN（列是 NOT NULL DEFAULT 'BUILTIN'，这里显式给值更直观）
        entity.setAgentMode(dto == null || dto.getAgentMode() == null || dto.getAgentMode().isBlank()
                ? DEFAULT_AGENT_MODE
                : dto.getAgentMode().trim());
        entity.setMessageCount(0);

        // insert 之后：自增 id 会被 MyBatis-Plus 回填到 entity，
        // createdAt / updatedAt 由 MetaObjectHandler 填 —— 所以不用再查一次库
        chatSessionMapper.insert(entity);
        log.info("新建会话：id={}, agentMode={}", entity.getId(), entity.getAgentMode());

        return toVo(entity);
    }

    /** 实体 → VO（字段目前一一对应；以后 VO 增删字段只改这里） */
    private ChatSessionVO toVo(ChatSession entity) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setAgentMode(entity.getAgentMode());
        vo.setMessageCount(entity.getMessageCount());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}