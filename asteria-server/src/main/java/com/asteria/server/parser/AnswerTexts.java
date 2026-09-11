package com.asteria.server.parser;

import java.util.regex.Pattern;

/**
 * 答案文本处理的小工具（包级私有，只有 {@code com.asteria.server.parser} 内部能用）。
 *
 * <p>抽成单独的类是为了避免 QuestionClassifier 和 AnswerNormalizer
 * 各写一份正则——同一个规则只留一处，改的时候不会漏。
 */
final class AnswerTexts {

    /** 答案里的字母（半角、全角都认） */
    private static final Pattern LETTER = Pattern.compile("[A-Za-zＡ-Ｚａ-ｚ]");

    /** 表示"正确"的词 */
    private static final Pattern TRUE_WORD = Pattern.compile("(?i)^(?:对|正确|是|√|✓|T|TRUE)$");

    /** 对错词全集（不在"正确"里的，都算"错误"） */
    private static final Pattern TRUE_FALSE_WORD =
            Pattern.compile("(?i)^(?:对|错|正确|错误|是|否|√|×|✓|✗|T|F|TRUE|FALSE)$");

    private AnswerTexts() {
    }

    /**
     * 截掉分号后面的内容。
     * 导出文件的答案带着原文，字母总在最前面，如 "D；超文本标记语言;"。
     */
    static String letterPart(String rawAnswer) {
        if (rawAnswer == null) {
            return "";
        }
        int cut = rawAnswer.indexOf('；');
        if (cut < 0) {
            cut = rawAnswer.indexOf(';');
        }
        return cut < 0 ? rawAnswer : rawAnswer.substring(0, cut);
    }

    /**
     * 抽出答案里的所有选项字母：去重 + 排序 + 拼接。
     * <p>"A、C" / "A,C" / "A C" / "AC" / "c、a" → 都是 "AC"
     */
    static String letters(String rawAnswer) {
        String part = letterPart(rawAnswer);
        if (part.isEmpty()) {
            return "";
        }
        return LETTER.matcher(part)
                .results()
                .map(m -> normalizeLetter(m.group()))
                .distinct()
                .sorted()
                .reduce("", String::concat);
    }

    /** 全角字母转半角并统一大写：Ａ→A、a→A */
    static String normalizeLetter(String letter) {
        char c = letter.charAt(0);
        if (c >= 'Ａ' && c <= 'Ｚ') {
            c = (char) (c - 'Ａ' + 'A');
        } else if (c >= 'ａ' && c <= 'ｚ') {
            c = (char) (c - 'ａ' + 'a');
        }
        return String.valueOf(Character.toUpperCase(c));
    }

    /** 是不是对错词（对/错/正确/错误/√/×/T/F…） */
    static boolean isTrueFalseWord(String text) {
        return text != null && TRUE_FALSE_WORD.matcher(text.trim()).matches();
    }

    /**
     * 对错词的语义：true = 正确，false = 错误，null = 不是对错词。
     * <p>判断顺序很重要：先判"正确"，剩下的才当"错误"——
     * 因为 TRUE_FALSE_WORD 里既有"对"也有"错"。
     */
    static Boolean trueFalseMeaning(String text) {
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
}
