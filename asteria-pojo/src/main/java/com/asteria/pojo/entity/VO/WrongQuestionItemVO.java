package com.asteria.pojo.entity.VO;

import lombok.Data;

/** 会话结果里的错题明细（对应接口文档 wrongQuestions[] 的元素） */
@Data
public class WrongQuestionItemVO {

    private Long questionId;

    /** 题干 */
    private String stem;

    private String type;

    /** 用户当时错答了什么 */
    private String userAnswer;

    /** 标准答案 */
    private String answer;
}
