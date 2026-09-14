package com.asteria.pojo.entity.DTO;

import lombok.Data;

/** 提交单题答案的请求体，对应接口文档「提交单题答案」的 Body */
@Data
public class AnswerSubmitDTO {

    /** 题目 id（必传）—— 必须属于当前会话，Service 会校验 */
    private Long questionId;

    /** 用户作答：单选A / 多选ACD / 判断A=正确B=错误 / 填空多空用；分隔 / 简答文本 */
    private String userAnswer;
}
