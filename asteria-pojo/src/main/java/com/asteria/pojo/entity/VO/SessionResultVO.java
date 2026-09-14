package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.util.List;

/** 会话结果 VO（完成页），对应接口文档「查询会话结果」的 data */
@Data
public class SessionResultVO {

    private Long sessionId;

    private Integer totalCount;

    private Integer answeredCount;

    private Integer correctCount;

    /**
     * 正确率，单位 <b>0~100</b> 的整数（按接口文档）。
     *
     * <p>注意：前端 PracticeView 目前当成 0~1 在用（formatPercent 会再乘 100），
     * 联调时需要把前端那两处改成 /100，否则会显示成 5000%。
     */
    private Integer accuracy;

    /** 按题型统计 */
    private List<TypeStatVO> typeStats;

    /** 错题明细（只含判为错的题） */
    private List<WrongQuestionItemVO> wrongQuestions;
}
