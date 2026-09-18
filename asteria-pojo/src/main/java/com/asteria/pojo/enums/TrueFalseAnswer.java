package com.asteria.pojo.enums;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 判断题答案的对外约定：<b>A = 正确、B = 错误</b>。
 *
 * <p>为什么单独抽一个枚举：这个约定同时被三处使用 ——
 * <ul>
 *   <li>文件导入时的答案归一化（{@code server/parser/AnswerNormalizer}）</li>
 *   <li>AI 补答案（{@code server/ai/QuestionAiEnricher}）</li>
 *   <li>前端刷题页（判断题固定渲染 A/B 两张卡片，用户点选提交的就是 A 或 B）</li>
 * </ul>
 * 以前三处各写一份，AI 那处写成了 {@code TRUE/FALSE}，而判分是
 * {@code std.equals(mine)} 全等比较，于是 {@code "TRUE".equals("A")} 恒为 false
 * —— AI 补过的判断题无论用户答什么都判错。约定只留这一份，以后不会再漏改。
 *
 * <p>「对错词」的识别规则也一并收在这里（{@link #meaningOf}），
 * 不再散落到各个包里。
 */
public enum TrueFalseAnswer {

    TRUE("A", "正确"),
    FALSE("B", "错误");

    /** 表示"正确"的词 */
    private static final Pattern TRUE_WORD =
            Pattern.compile("(?i)^(?:对|正确|是|√|✓|T|TRUE)$");

    /** 对错词全集（不在"正确"里的，都算"错误"） */
    private static final Pattern TRUE_FALSE_WORD =
            Pattern.compile("(?i)^(?:对|错|正确|错误|是|否|√|×|✓|✗|T|F|TRUE|FALSE)$");

    /** 存进 question.answer 的值，也是前端提交的取值 */
    private final String key;

    /** 给人看的中文名，用于提示词和日志 */
    private final String label;

    TrueFalseAnswer(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    /** 是不是对错词（对/错/正确/错误/√/×/T/F…） */
    public static boolean isWord(String text) {
        return text != null && TRUE_FALSE_WORD.matcher(text.trim()).matches();
    }

    /**
     * 对错词的语义：true = 正确，false = 错误，null = 不是对错词。
     *
     * <p>判断顺序很重要：先判"正确"，剩下的才当"错误" ——
     * 因为 {@code TRUE_FALSE_WORD} 里既有"对"也有"错"。
     */
    public static Boolean meaningOf(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (TRUE_WORD.matcher(s).matches()) {
            return Boolean.TRUE;
        }
        if (TRUE_FALSE_WORD.matcher(s).matches()) {
            return Boolean.FALSE;
        }
        return null;
    }

    /** 语义 → 约定值（A/B）；语义为 null（不是对错词）时返回 null */
    public static String keyOfMeaning(Boolean meaning) {
        if (meaning == null) {
            return null;
        }
        return meaning ? TRUE.key : FALSE.key;
    }

    /**
     * 任意写法 → 约定值（A/B）；识别不了返回 null。
     *
     * <p>同时认三类输入：约定值本身（A/B，大小写不限）、对错词（对/错/√/×/T/F…）、
     * 以及它们的变体。给 AI 补答案用 —— 模型吐哪种都能收敛到 A/B。
     */
    public static String keyOf(String text) {
        if (text == null) {
            return null;
        }
        String v = text.trim().toUpperCase(Locale.ROOT);
        if (TRUE.key.equals(v)) {
            return TRUE.key;
        }
        if (FALSE.key.equals(v)) {
            return FALSE.key;
        }
        return keyOfMeaning(meaningOf(v));
    }
}
