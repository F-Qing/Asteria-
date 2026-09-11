package com.asteria.common.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class QuestionParser {
    /** 题号行：① 【第 1 题】 ② 1、 或 1. 开头 */
    public static final Pattern QUESTION_HEADER =
            Pattern.compile("^\\s*【\\s*第\\s*\\d+\\s*题\\s*】|^\\s*\\d+\\s*[、.．]\\s*\\S");

    /** 题型：xxx（常和题号写在同一行） */
    private static final Pattern TYPE_PATTERN =
            Pattern.compile("题型[：:]\\s*(\\S+)");

    /** 题干：题目：xxx / 题干：xxx */
    private static final Pattern STEM_PATTERN =
            Pattern.compile("^\\s*(?:题目|题干)\\s*[:：]\\s*(.+)$");

    /** 选项标签行：选项 / 选项： / 选项:（冒号后可能有多余内容，如“选项：c”） */
    private static final Pattern OPTION_LABEL_PATTERN =
            Pattern.compile("^\\s*选项\\s*[:：]?\\s*(.*)$");

    /** 选项行：A. xxx / A、xxx / Ａ．xxx（兼容全角字母） */
    private static final Pattern OPTION_PATTERN =
            Pattern.compile("^\\s*([A-Za-zＡ-Ｚａ-ｚ])\\s*[.、．)）]\\s*(.+)$");

    /** 我的答案：xxx —— 直接忽略（做题人写的，不是标准答案） */
    private static final Pattern MY_ANSWER_PATTERN =
            Pattern.compile("^\\s*我的答案\\s*[:：].*$");

    /** 带标签的答案行：正确答案 / 参考答案 / 答案，取冒号后**整段原文**（不截断、不归一化） */
    private static final Pattern ANSWER_LABEL_PATTERN =
            Pattern.compile("^\\s*(?:正确答案|参考答案|答案)\\s*[:：]\\s*(.+)$");

    /**
     * 行内找答案：兼容"我的答案：xxx    正确答案：yyy"写在同一行的情况（用 find 搜索，不要求行首）。
     * <p>只认"正确答案/参考答案"，**故意不认单独的"答案"** ——
     * 因为"我的答案："里也含"答案"两个字，认了就会把做题人的答案当成标准答案。
     */
    private static final Pattern ANSWER_INLINE_PATTERN =
            Pattern.compile("(?:正确答案|参考答案)\\s*[:：]\\s*(.+)$");

    /** 分隔线：------ / ====== / ~~~~~~ */
    private static final Pattern SEPARATOR_PATTERN =
            Pattern.compile("^\\s*[-=~_*]{3,}\\s*$");

    /** 裸答案行（对错版）：整行只有对错词 */
    private static final Pattern BARE_TRUE_FALSE_PATTERN =
            Pattern.compile("(?i)^(?:对|错|正确|错误|是|否|√|×|✓|✗|T|F|TRUE|FALSE)$");

    /** 裸答案行（字母版）：整行只有选项字母和分隔符 */
    private static final Pattern BARE_LETTERS_PATTERN =
            Pattern.compile("^[A-Za-zＡ-Ｚａ-ｚ]+(?:\\s*[、,，;；/]\\s*[A-Za-zＡ-Ｚａ-ｚ]+)*$");

    /** 裸答案不会超过这个长度（再长肯定不是答案） */
    private static final int BARE_ANSWER_MAX_LENGTH = 20;

    /** 入口一：整份文本 → 题目列表 */
    public List<RawQuestion> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return parseLines(text.lines().toList());
    }




    /** 入口二：按行切块（核心） */
    public List<RawQuestion> parseLines(List<String> lines) {
        List<RawQuestion> questions = new ArrayList<>();   // 已收好的题（结果）
        List<String> block = new ArrayList<>();            // 手里的夹子（当前这一道题）
        int blockStartLine = 0;                            // 当前题从第几行开始；0 = 还没进入题目区

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean isHeader = QUESTION_HEADER.matcher(line).find();

            if (isHeader) {
                // ① 手里还有上一道题 → 先交出去
                if (!block.isEmpty()) {
                    questions.add(buildQuestion(block, blockStartLine));
                    block.clear();                          // 腾空夹子
                }
                // ② 记住新这道题从第几行开始（行号从 1 开始）
                blockStartLine = i + 1;
            }

            // ③ 已经进入题目区 → 这行夹进当前这道题
            if (blockStartLine > 0) {
                block.add(line);
            }
        }

        // ④ 循环结束后，最后一道题还在夹子里，补收一次
        if (!block.isEmpty()) {
            questions.add(buildQuestion(block, blockStartLine));
        }
        return questions;
    }

    /** 把"一道题的所有行"提取成 RawQuestion（阶段一：只取原始材料，不判型、不归一化答案） */
    private RawQuestion buildQuestion(List<String> block, int startLine) {
        String rawType = null;
        String rawStem = null;
        List<QuestionOption> rawOptions = new ArrayList<>();
        String rawAnswer = null;
        List<String> suspicious = new ArrayList<>();

        for (String line : block) {
            // 0) 空行 / 分隔线：跳过，不算可疑
            if (line.isBlank() || SEPARATOR_PATTERN.matcher(line).matches()) {
                continue;
            }

            // 1) 题型（常和题号同一行）
            if (rawType == null) {
                var m = TYPE_PATTERN.matcher(line);
                if (m.find()) {
                    rawType = m.group(1).trim();
                    continue;
                }
            }

            // 2) 题干
            if (rawStem == null) {
                var m = STEM_PATTERN.matcher(line);
                if (m.matches()) {
                    rawStem = m.group(1).trim();
                    continue;
                }
            }

            // 3) 选项标签行：只是个"标签"，不当开关用（有的文件根本没有"选项："这一行）
            //    冒号后若有多余内容（如"选项：c"）记一条可疑
            var labelMatcher = OPTION_LABEL_PATTERN.matcher(line);
            if (labelMatcher.matches()) {
                String rest = labelMatcher.group(1).trim();
                if (!rest.isEmpty()) {
                    suspicious.add("选项标签行有异常内容：" + line.trim());
                }
                continue;
            }

            // 4) 选项行：靠"字母 + 分隔符"这个特征识别，不依赖"选项："标签
            var optionMatcher = OPTION_PATTERN.matcher(line);
            if (optionMatcher.matches()) {
                rawOptions.add(new QuestionOption(normalizeLetter(optionMatcher.group(1)),
                        optionMatcher.group(2).trim()));
                continue;
            }

            // 5) 带标签的答案：先找（有的文件把"正确答案"和"我的答案"写在**同一行**）
            if (rawAnswer == null) {
                var inline = ANSWER_INLINE_PATTERN.matcher(line);
                if (inline.find()) {
                    rawAnswer = inline.group(1).trim();
                    continue;
                }
                var anchored = ANSWER_LABEL_PATTERN.matcher(line);
                if (anchored.matches()) {
                    rawAnswer = anchored.group(1).trim();
                    continue;
                }
            }

            // 6) 只剩"我的答案"的行（做题人写的，不可信）：跳过
            if (MY_ANSWER_PATTERN.matcher(line).matches()) {
                continue;
            }

            // 7) 裸答案行：选项块之后，整行只有答案（字母必须在本题选项里）
            if (rawAnswer == null && !rawOptions.isEmpty() && isBareAnswer(line.trim(), rawOptions)) {
                rawAnswer = line.trim();
                continue;
            }

            // 8) 以上都不匹配 → 记入可疑
            suspicious.add(line);
        }

        return new RawQuestion(rawType, rawStem, rawOptions, rawAnswer, startLine, suspicious);
    }

    /** 是不是"裸答案行"：整行只有对错词，或整行只有本题选项里的字母 */
    private boolean isBareAnswer(String text, List<QuestionOption> options) {
        if (text.isEmpty() || text.length() > BARE_ANSWER_MAX_LENGTH) {
            return false;
        }
        if (BARE_TRUE_FALSE_PATTERN.matcher(text).matches()) {
            return true;
        }
        if (!BARE_LETTERS_PATTERN.matcher(text).matches() || options.isEmpty()) {
            return false;
        }
        // 字母必须都在本题选项里，防止把正文（如 "HTML"）误当答案
        List<String> keys = new ArrayList<>();
        for (QuestionOption option : options) {
            keys.add(option.getKey());
        }
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) && !keys.contains(normalizeLetter(String.valueOf(c)))) {
                return false;
            }
        }
        return true;
    }

    /** 全角字母转半角并统一大写：Ａ→A、a→A */
    private String normalizeLetter(String letter) {
        char c = letter.charAt(0);
        if (c >= 'Ａ' && c <= 'Ｚ') {
            c = (char) (c - 'Ａ' + 'A');
        } else if (c >= 'ａ' && c <= 'ｚ') {
            c = (char) (c - 'ａ' + 'a');
        }
        return String.valueOf(Character.toUpperCase(c));
    }

    //测试方法
    public void debugHeader(List<String> lines) {
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (QUESTION_HEADER.matcher(line).find()) {
                count++;
                System.out.println("第 " + (i + 1) + " 行是题号行: " + line.trim()/* 打印整行内容*/);
            }
        }
        System.out.println("共 " + count + " 个题号行");
    }
}
