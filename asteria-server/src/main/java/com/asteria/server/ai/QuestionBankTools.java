package com.asteria.server.ai;

import com.asteria.pojo.entity.Bank;
import com.asteria.pojo.entity.Chapter;
import com.asteria.pojo.entity.DTO.BankCountDTO;
import com.asteria.pojo.entity.Question;
import com.asteria.pojo.entity.VO.QuestionOptionVO;
import com.asteria.pojo.enums.QuestionType;
import com.asteria.server.mapper.BankMapper;
import com.asteria.server.mapper.ChapterMapper;
import com.asteria.server.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 给 AI 用的"题库查询工具"。
 *
 * <p>这个类前端看不到，只被模型调用：Spring AI 会把每个 {@code @Tool} 方法的
 * 名字 + description + 参数 schema 塞进请求，模型决定调哪个，再回调这里执行，
 * 把返回值当 tool 消息发回去。整个过程前端无感（chunk 事件只在最后回答时才出现）。
 *
 * <p><b>方法名不能说改就改</b>：Spring AI 默认拿 Java 方法名当工具名，
 * 而 {@link com.asteria.server.Services.impl.ChatServiceImpl} 的系统提示词里
 * 写死了 listBanks / searchQuestions / getQuestionDetail 三个名字。改名要两边一起改，
 * 或者在 {@code @Tool(name = "...")} 里显式钉死。
 *
 * <p><b>每个方法都用 try/catch 包住</b>：工具里抛异常会让整条 SSE 流变成 error 事件，
 * 用户看到的是"AI 出错了"，而不是"这道题没搜到"。转成一句人话回给模型，它还能接着答。
 *
 * <p><b>返回"给人看的文本"而不是 JSON</b>：一行一题的紧凑文本最省 token，
 * 而且模型对"答案：xxx"这种字段边界，比嵌套 JSON 更不容易看错。
 */
@Component
@Slf4j
public class QuestionBankTools {

    /** 模型没填 limit 时返回几道题 */
    private static final int DEFAULT_LIMIT = 5;
    /** limit 上限：模型说破天也只给这么多（description 里写"最大 20"是提示，这里是强制） */
    private static final int MAX_LIMIT = 20;
    /** 搜索结果里单条题干的截断长度；超了结尾加「…」，模型看到省略号就知道要调 getQuestionDetail 看全 */
    private static final int MAX_STEM_CHARS = 1000;

    private final QuestionMapper questionMapper;
    private final BankMapper bankMapper;
    private final ChapterMapper chapterMapper;
    private final ObjectMapper objectMapper;

    public QuestionBankTools(QuestionMapper questionMapper, BankMapper bankMapper,
                             ChapterMapper chapterMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.bankMapper = bankMapper;
        this.chapterMapper = chapterMapper;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "列出系统里所有题库：id、名称、题目数量。不确定用户的题目在哪个题库时，先调这个。")
    public String listBanks() {
        log.info("AI 调用工具：listBanks()");
        try {
            List<Bank> banks = bankMapper.selectList(Wrappers.<Bank>lambdaQuery().orderByAsc(Bank::getId));
            if (banks.isEmpty()) {
                return "题库是空的：用户还没有导入任何题库。";
            }

            // 一次查完所有题库的题目数再累加，避免 N 个题库查 N 次
            Map<Long, Integer> countMap = new HashMap<>();
            List<Long> bankIds = banks.stream().map(Bank::getId).toList();
            for (BankCountDTO row : questionMapper.countGroupByBankAndType(bankIds)) {
                countMap.merge(row.getBankId(), row.getCnt(), Integer::sum);
            }

            StringBuilder sb = new StringBuilder("共 ").append(banks.size()).append(" 个题库：\n");
            for (Bank bank : banks) {
                // 不输出题库 id：searchQuestions 只认题库名，id 对模型没用，
                // 一旦出现在结果里，模型就容易顺嘴写进给用户的回答
                sb.append("- 《").append(bank.getName()).append("》（")
                  .append(countMap.getOrDefault(bank.getId(), 0)).append(" 题）\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return fail("listBanks", e);
        }
    }

    @Tool(description = "按关键词搜索题干，返回题目 id、题型、题干、答案。关键词至少 2 个字；"
            + "不指定题库名时会在所有题库里搜。要讲某道题之前先用它拿到题目 id。")
    public String searchQuestions(
            @ToolParam(description = "题干里会出现的关键词，例如“二叉树”“三次握手”") String keyword,
            @ToolParam(description = "题库名称，可以只写一部分，不填=所有题库", required = false) String bankName,
            @ToolParam(description = "题型：SINGLE 单选 / MULTIPLE 多选 / TRUE_FALSE 判断 / "
                    + "ESSAY 简答 / FILL_BLANK 填空，不填=不限", required = false) String type,
            @ToolParam(description = "最多返回几道题，默认 5，最大 20", required = false) Integer limit) {

        log.info("AI 调用工具：searchQuestions(keyword={}, bankName={}, type={}, limit={})",
                keyword, bankName, type, limit);
        try {
            String kw = keyword == null ? "" : keyword.trim();
            if (kw.length() < 2) {
                return "关键词太短（至少 2 个字），请让用户说得更具体，或者先调 listBanks 看看有哪些题库。";
            }

            // 题库名 → id 集合；命中 0 个就直接告诉模型，别让它自己猜
            List<Long> bankIds = null;
            if (bankName != null && !bankName.isBlank()) {
                bankIds = bankMapper.selectList(Wrappers.<Bank>lambdaQuery()
                                .like(Bank::getName, bankName.trim()))
                        .stream().map(Bank::getId).toList();
                if (bankIds.isEmpty()) {
                    return "没有名字包含“" + bankName + "”的题库，先用 listBanks 看看有哪些题库。";
                }
            }

            String typeCode = normalizeType(type);
            int size = (limit == null || limit < 1) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

            LambdaQueryWrapper<Question> wrapper = Wrappers.<Question>lambdaQuery()
                    .like(Question::getStem, kw)
                    .in(bankIds != null, Question::getBankId, bankIds)
                    .eq(typeCode != null, Question::getType, typeCode);

            // 先 COUNT 再拼 LIMIT：顺序反了 LIMIT 会被算进 COUNT 语句，返回的总数就永远是 size
            Long total = questionMapper.selectCount(wrapper);
            wrapper.orderByAsc(Question::getId).last("LIMIT " + size);   // size 是算出来的 int，没有注入面
            List<Question> rows = questionMapper.selectList(wrapper);
            if (rows.isEmpty()) {
                return "没有找到题干包含“" + kw + "”的题目。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共命中 ").append(total).append(" 道，列出前 ").append(rows.size()).append(" 道：\n");
            // 这句必须写在结果里：模型对"工具返回内容"里的指令比系统提示词更敏感，
            // 只靠系统提示词约束，它照样会把 id 念给用户听
            sb.append("（每行开头的 id 是给你调 getQuestionDetail 用的内部编号，"
                    + "回答用户时不要写出来）\n");
            for (Question question : rows) {
                sb.append("- id=").append(question.getId())
                  .append("　[").append(chineseType(question.getType())).append("] ")
                  .append(truncate(question.getStem(), MAX_STEM_CHARS));
                String answer = question.getAnswer();
                sb.append(answer == null || answer.isBlank() ? "　（本题未提供答案）" : "　答案：" + answer);
                sb.append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return fail("searchQuestions", e);
        }
    }

    @Tool(description = "查看某道题的完整内容：选项、标准答案、解析、知识点、所属题库与章节。"
            + "参数必须是 searchQuestions 返回的题目 id。")
    public String getQuestionDetail(
            @ToolParam(description = "题目 id（来自 searchQuestions 的结果）") Long questionId) {

        log.info("AI 调用工具：getQuestionDetail(questionId={})", questionId);
        try {
            if (questionId == null) {
                return "缺少题目 id。";
            }
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                return "不存在 id=" + questionId + " 的题目，请用 searchQuestions 返回的 id。";
            }

            Bank bank = question.getBankId() == null ? null : bankMapper.selectById(question.getBankId());
            Chapter chapter = question.getChapterId() == null ? null : chapterMapper.selectById(question.getChapterId());

            StringBuilder sb = new StringBuilder();
            // 不输出题目 id：模型是按 id 调进来的，它自己知道是哪道题，写出来只会被念给用户
            sb.append("题型：").append(chineseType(question.getType()))
              .append("　题库：").append(bank == null ? "未知" : "《" + bank.getName() + "》")
              .append("　章节：").append(chapter == null ? "无" : chapter.getName())
              .append('\n');
            // 这里不截断：要讲题就必须让模型看到完整题干
            sb.append("题干：").append(question.getStem()).append('\n');

            for (QuestionOptionVO option : readOptions(question.getOptions())) {
                sb.append(option.getKey()).append("．").append(option.getText()).append('\n');
            }

            sb.append("标准答案：").append(blankToDash(question.getAnswer())).append('\n');
            sb.append("已有解析：").append(blankToDash(question.getAnalysis())).append('\n');
            sb.append("知识点：").append(readKnowledgePoints(question.getKnowledgePoints())).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return fail("getQuestionDetail", e);
        }
    }

    // ========== 下面是给上面三个方法打杂的，不暴露给模型 ==========

    /**
     * 题型容错：模型可能传 SINGLE，也可能传"单选题"或者"single"。
     * 认不出来返回 null（= 不限题型），不抛异常 —— 抛了整条流就断了。
     */
    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String value = type.trim();
        QuestionType byChinese = QuestionType.ofChinese(value);
        if (byChinese != null) {
            return byChinese.name();
        }
        try {
            return QuestionType.valueOf(value.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            log.warn("AI 传了认不出的题型，按不限处理：{}", value);
            return null;
        }
    }

    private String chineseType(String type) {
        if (type == null) {
            return "未知题型";
        }
        try {
            return QuestionType.valueOf(type).getChineseName();
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    private List<QuestionOptionVO> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOptionVO>>() {});
        } catch (Exception e) {
            log.warn("选项 JSON 解析失败：{}", e.getMessage());
            return List.of();
        }
    }

    /** 知识点是 JSON 数组，拼成顿号分隔的一行给模型看 */
    private String readKnowledgePoints(String json) {
        if (json == null || json.isBlank()) {
            return "无";
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list.isEmpty() ? "无" : String.join("、", list);
        } catch (Exception e) {
            return "无";
        }
    }

    /** 空值 →「（未提供）」：必须显式说明，否则模型会当成"答案就是空的"，然后自己编一个 */
    private String blankToDash(String text) {
        return text == null || text.isBlank() ? "（未提供）" : text;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String fail(String tool, Exception e) {
        log.warn("工具 {} 执行失败：{}", tool, e.getMessage());
        return "执行 " + tool + " 时出错了：" + e.getMessage();
    }
}
