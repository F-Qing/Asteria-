package com.asteria.pojo.entity.VO;

import lombok.Data;

/**
 * 刷题会话返回体，字段与接口文档「创建刷题会话」的 data 一一对应。
 *
 * <p>刻意不直接把 practice_session 实体返回去：实体里有 sessionSource / createdAt
 * 这些前端用不上的列，接口返回的东西就应该跟文档对齐，多一个字段都是负担。
 */
@Data
public class PracticeSessionVO {

    /** 会话 id（前端拿它跳 /practice/{id}） */
    private Long id;

    /** 题库 id */
    private Long bankId;

    /** SEQUENTIAL/RANDOM */
    private String mode;

    /** 题型 */
    private String questionType;

    /** 章节 id（全章节为 null） */
    private Long chapterId;

    /** IN_PROGRESS/COMPLETED */
    private String status;

    /** 题目总数 */
    private Integer totalCount;

    /** 已答题数 */
    private Integer answeredCount;

    /** 答对数 */
    private Integer correctCount;
}
