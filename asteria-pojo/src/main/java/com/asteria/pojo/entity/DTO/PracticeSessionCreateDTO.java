package com.asteria.pojo.entity.DTO;

import lombok.Data;

/**
 * 创建刷题会话的请求体，对应接口文档「创建刷题会话」的 Body。
 *
 * <p>几个字段都是"前端给什么就收什么"，取值合法性在 Service 层校验，
 * 不在这里用注解校验（项目里其它模块也是手写校验，保持一致）。
 */
@Data
public class PracticeSessionCreateDTO {

    /** 题库 id（必传） */
    private Long bankId;

    /** 出题顺序：SEQUENTIAL 顺序 / RANDOM 随机；不传按 SEQUENTIAL */
    private String mode;

    /** 题型：SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK/ALL；不传按 ALL（全部题型） */
    private String questionType;

    /** 章节 id；不传或 null = 全章节 */
    private Long chapterId;
}
