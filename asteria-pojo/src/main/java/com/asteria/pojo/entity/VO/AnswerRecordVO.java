package com.asteria.pojo.entity.VO;

import lombok.Data;

/** 会话里"这道题答过了"的记录（对应接口文档 records[] 的元素） */
@Data
public class AnswerRecordVO {

    private Long questionId;

    private String userAnswer;

    /** true 对 / false 错 / null 未判分（简答题）—— 三态，所以用包装类型 Boolean */
    private Boolean isCorrect;
}
