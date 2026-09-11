package com.asteria.pojo.enums;

/**
 * 考试紧急程度等级枚举。
 * 每个枚举项自带前端要的字符串值（red/orange/green），
 * 判断逻辑集中在这里，VO/Service 不散落魔法字符串。
 */
public enum ExamLevel {

    RED("red"),
    ORANGE("orange"),
    GREEN("green");

    private final String value;

    ExamLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 按剩余天数选取等级：≤7 天红、≤14 天橙、其余绿 */
    public static ExamLevel of(long daysLeft) {
        if (daysLeft <= 7) return RED;
        if (daysLeft <= 14) return ORANGE;
        return GREEN;
    }
}