package com.asteria.pojo.enums;

/**
 * 题型枚举。
 *
 * <p>每个枚举项自带两个东西：接口对外的英文值（SINGLE 等，与接口文档一致）和源文件里的中文题型名（单选题 等）。
 * 解析 txt 时用中文名反查枚举，避免在解析代码里散落中文字符串。
 */
public enum QuestionType {

    SINGLE("单选题"),
    MULTIPLE("多选题"),
    TRUE_FALSE("判断题"),
    ESSAY("简答题"),
    FILL_BLANK("填空题");

    private final String chineseName;

    QuestionType(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getChineseName() {
        return chineseName;
    }

    /** 中文题型名 → 枚举；认不出来返回 null，由调用方决定是跳过该题还是整批失败 */
    public static QuestionType ofChinese(String chineseName) {
        if (chineseName == null) {
            return null;
        }
        String name = chineseName.trim();
        for (QuestionType type : values()) {
            if (type.chineseName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
