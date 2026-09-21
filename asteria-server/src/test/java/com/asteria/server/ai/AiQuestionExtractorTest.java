package com.asteria.server.ai;

import com.asteria.common.Tool.QuestionOption;
import com.asteria.common.Tool.RawQuestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 结构化抽取的「解析 + 逐题校验」单测。
 *
 * <p>不碰网络：只喂模型可能返回的 JSON 文本，验证程序这边的兜底闸门——
 * 这正是"不让垃圾进库"的那一层，必须有测试盯着。
 */
class AiQuestionExtractorTest {

    private AiQuestionExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new AiQuestionExtractor();
        ReflectionTestUtils.setField(extractor, "objectMapper", new ObjectMapper());
    }

    @Test
    @DisplayName("标准返回：字段正确映射成题目")
    void mapsStandardPayload() throws Exception {
        String json = """
                {"questions":[
                  {"chapter":"习 题 1","type":"单选题","stem":"Python语言属于以下哪种语言？",
                   "options":[{"key":"A","content":"机器语言"},{"key":"B","content":"高级语言"}],
                   "answer":"B"}
                ]}
                """;

        AiQuestionExtractor.Parsed parsed = extractor.parseAndValidate(json, 7);

        assertEquals(1, parsed.questions().size());
        RawQuestion q = parsed.questions().get(0);
        assertEquals("Python语言属于以下哪种语言？", q.getRawStem());
        assertEquals("习 题 1", q.getChapterName());
        assertEquals("单选题", q.getRawType());
        assertEquals("B", q.getRawAnswer());
        assertEquals(2, q.getRawOptions().size());
        assertEquals("A", q.getRawOptions().get(0).getKey());
        assertEquals("机器语言", q.getRawOptions().get(0).getText());
        assertEquals(7, q.getStartLine());
    }

    @Test
    @DisplayName("模型爱包 markdown 代码块、爱在前面说一句 → 仍然能解析")
    void toleratesFenceAndProse() throws Exception {
        String json = """
                好的，以下是整理后的 JSON：
                ```json
                [{"type":"简答题","stem":"简述程序的编译方式和解释方式的区别。","options":[],"answer":""}]
                ```
                """;

        AiQuestionExtractor.Parsed parsed = extractor.parseAndValidate(json, 1);

        assertEquals(1, parsed.questions().size());
        assertEquals("简述程序的编译方式和解释方式的区别。", parsed.questions().get(0).getRawStem());
        assertTrue(parsed.questions().get(0).getRawOptions().isEmpty());
        assertNull(parsed.questions().get(0).getRawAnswer(), "空答案应当是 null，后面会落成空串");
    }

    @Test
    @DisplayName("根对象里数组字段叫 data 也行")
    void acceptsAnyArrayFieldName() throws Exception {
        String json = "{\"data\":[{\"stem\":\"题干\",\"type\":\"判断题\",\"options\":[],\"answer\":\"A\"}]}";

        assertEquals(1, extractor.parseAndValidate(json, 1).questions().size());
    }

    @Test
    @DisplayName("没有题干的题直接丢弃，不让它走到入库阶段")
    void dropsQuestionWithoutStem() throws Exception {
        String json = """
                {"questions":[
                  {"type":"单选题","stem":"   ","options":[{"key":"A","content":"甲"}],"answer":"A"},
                  {"type":"单选题","stem":"正常题干","options":[{"key":"A","content":"甲"}],"answer":"A"}
                ]}
                """;

        AiQuestionExtractor.Parsed parsed = extractor.parseAndValidate(json, 1);

        assertEquals(1, parsed.questions().size());
        assertEquals("正常题干", parsed.questions().get(0).getRawStem());
        assertEquals(1, parsed.noStem());
    }

    @Test
    @DisplayName("答案字母不在选项里 → 丢答案、保题目（宁可没答案，也不要错答案）")
    void dropsAnswerThatIsNotInOptions() throws Exception {
        String json = """
                {"questions":[
                  {"type":"单选题","stem":"题干","options":[{"key":"A","content":"甲"},{"key":"B","content":"乙"}],"answer":"D"}
                ]}
                """;

        AiQuestionExtractor.Parsed parsed = extractor.parseAndValidate(json, 1);

        assertEquals(1, parsed.questions().size());
        assertNull(parsed.questions().get(0).getRawAnswer());
        assertEquals(1, parsed.badAnswer());
    }

    @Test
    @DisplayName("多选题答案字母都在选项里 → 保留原样（做不做归一化是后面 AnswerNormalizer 的事）")
    void keepsMultiAnswerWhenLettersExist() throws Exception {
        String json = """
                {"questions":[
                  {"type":"多选题","stem":"题干","options":[
                     {"key":"A","content":"甲"},{"key":"B","content":"乙"},{"key":"C","content":"丙"},{"key":"D","content":"丁"}],
                   "answer":"A、C"}
                ]}
                """;

        AiQuestionExtractor.Parsed parsed = extractor.parseAndValidate(json, 1);

        assertEquals("A、C", parsed.questions().get(0).getRawAnswer());
        assertEquals(0, parsed.badAnswer());
    }

    @Test
    @DisplayName("答案是一段文字（简答题）→ 不做字母校验，原样保留")
    void keepsTextAnswerUntouched() throws Exception {
        String json = """
                {"questions":[
                  {"type":"简答题","stem":"简述区别。","options":[],"answer":"编译是整体转换，解释是逐条转换。"}
                ]}
                """;

        AiQuestionExtractor.Parsed parsed = extractor.parseAndValidate(json, 1);

        assertEquals("编译是整体转换，解释是逐条转换。", parsed.questions().get(0).getRawAnswer());
        assertEquals(0, parsed.badAnswer());
    }

    @Test
    @DisplayName("选项：全角字母归一化成半角、重复字母与空内容丢弃")
    void normalizesOptions() throws Exception {
        String json = """
                {"questions":[
                  {"type":"单选题","stem":"题干","answer":"A","options":[
                     {"key":"Ａ","content":"甲"},
                     {"key":"A","content":"重复的 A"},
                     {"key":"B","content":"   "},
                     {"key":"C","content":"丙"},
                     {"key":"选项四","content":"非法字母"}
                  ]}
                ]}
                """;

        AiQuestionExtractor.Parsed parsed = extractor.parseAndValidate(json, 1);
        List<QuestionOption> options = parsed.questions().get(0).getRawOptions();

        assertEquals(2, options.size());
        assertEquals("A", options.get(0).getKey());
        assertEquals("甲", options.get(0).getText());
        assertEquals("C", options.get(1).getKey());
        assertEquals(3, parsed.badOption());
    }

    @Test
    @DisplayName("模型返回的不是题目数组 → 抛异常（外层会重试，不静默通过）")
    void failsOnNonArrayPayload() {
        String json = "{\"message\":\"抱歉，我无法完成\"}";

        Exception e = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> extractor.parseAndValidate(json, 1));
        assertNotNull(e.getMessage());
    }
}
