package com.asteria.server.parser;

import com.asteria.common.Tool.QuestionOption;
import com.asteria.common.Tool.RawQuestion;
import com.asteria.pojo.enums.QuestionType;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 答案归一化器：把答案**原文**变成能入库的**规范值**。
 *
 * <pre>
 * 单选  "D；超文本标记语言;"     → "D"
 * 多选  "A、C" / "A,C" / "AC"   → "AC"（去重 + 排序 + 无分隔符）
 * 判断  "对" → "A"，"错" → "B"   → 按前端约定：A=正确、B=错误
 * 填空  "北京；上海"             → 原样（多个空用分号分隔）
 * 简答  "一段话…"                → 原样
 * </pre>
 */
@Component
public class AnswerNormalizer {

    /** 前端判断题两张卡的固定约定：A = 正确、B = 错误 */
    private static final String TRUE_KEY = "A";
    private static final String FALSE_KEY = "B";

    /** 归一化；答案缺失时返回 null（调用方据此跳过该题并记入报告） */
    public String normalize(RawQuestion raw, QuestionType type) {
        if (type == null || raw.getRawAnswer() == null || raw.getRawAnswer().isBlank()) {
            return null;
        }
        return switch (type) {
            case SINGLE -> normalizeSingle(raw);
            case MULTIPLE -> normalizeMultiple(raw);
            case TRUE_FALSE -> normalizeTrueFalse(raw);
            case FILL_BLANK, ESSAY -> raw.getRawAnswer().trim();
        };
    }

    /** 单选：取第一个字母（"D；超文本标记语言;" → "D"） */
    private String normalizeSingle(RawQuestion raw) {
        String letters = AnswerTexts.letters(raw.getRawAnswer());
        return letters.isEmpty() ? null : letters.substring(0, 1);
    }

    /** 多选：所有字母去重排序拼接（"A、C" → "AC"） */
    private String normalizeMultiple(RawQuestion raw) {
        String letters = AnswerTexts.letters(raw.getRawAnswer());
        return letters.isEmpty() ? null : letters;
    }
    /**
     * 判断：先判原文语义，再按前端约定输出 A/B。
     *
     * <p>⚠️ 原文是字母时（如 "B"），必须**先查出该选项的文本**再判语义——
     * 有的文件选项顺序是 "A.错 B.对"，这时答案 B 表示的是"正确"，
     * 直接写死 A=对就会把对错判反。
     */
    private String normalizeTrueFalse(RawQuestion raw) {
        String rawAnswer = raw.getRawAnswer().trim();

        // 1) 原文本身是对错词（对/错/√/×/T/F…）
        Boolean meaning = AnswerTexts.trueFalseMeaning(rawAnswer);

        // 2) 原文是字母 → 查选项文本，再判语义
        if (meaning == null) {
            String letter = normalizeSingle(raw);
            if (letter == null) {
                return null;
            }
            meaning = AnswerTexts.trueFalseMeaning(findOptionText(raw.getRawOptions(), letter));
        }

        if (meaning == null) {
            return null;
        }
        return meaning ? TRUE_KEY : FALSE_KEY;
    }

    /** 按选项字母找出选项内容 */
    private String findOptionText(List<QuestionOption> options, String key) {
        for (QuestionOption option : options) {
            if (key.equals(option.getKey())) {
                return option.getText();
            }
        }
        return null;
    }
}