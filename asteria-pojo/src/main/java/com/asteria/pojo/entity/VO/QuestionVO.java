package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.util.List;

/** 题目列表项（字段与接口文档「题目分页查询」的 list 元素一一对应） */
@Data
public class QuestionVO {

    private Long id;

    /** 所属题库 id */
    private Long bankId;

    /** 所属章节 id */
    private Long chapterId;

    /** 章节名称（chapter 表里查出来的，不是 question 的列） */
    private String chapterName;

    /** 题型：SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK */
    private String type;

    /** 题干 */
    private String stem;

    /** 选项（判断题/填空题/简答题为 null 或空） */
    private List<QuestionOptionVO> options;

    /** 标准答案 */
    private String answer;

    /** 答案解析（现在库里大多是空，接 AI 后填） */
    private String analysis;

    /** 知识点标签（JSON 数组，接 AI 后填） */
    private List<String> knowledgePoints;
}
