package com.asteria.pojo.entity.VO;

import lombok.Data;

/**
 * 判题结果 VO，字段与接口文档「提交单题答案」的 data 一一对应。
 *
 * <p>注意 answer / analysis 是在<b>这个接口</b>才返回的 ——
 * 会话详情里刻意不给（防偷看），提交完才揭晓。
 */
@Data
public class AnswerSubmitResultVO {

    private Long questionId;

    private String userAnswer;

    /** true 对 / false 错 / null 不判分（简答题） */
    private Boolean isCorrect;

    /** 标准答案（提交后才给） */
    private String answer;

    /** 答案解析 */
    private String analysis;

    /** 更新后的已答题数 */
    private Integer answeredCount;

    /** 更新后的答对数 */
    private Integer correctCount;

    /** 更新后的会话状态：IN_PROGRESS/COMPLETED */
    private String sessionStatus;
}
