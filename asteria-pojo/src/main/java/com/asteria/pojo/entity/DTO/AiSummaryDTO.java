package com.asteria.pojo.entity.DTO;

import lombok.Data;

/**
 * 生成知识点总结的请求体，对应接口文档「生成知识点总结」的 Body。
 *
 * <p>和 VO 的分工：DTO 收前端传进来的东西（这里只有两个字段），
 * {@code AiSummaryVo} 是返回给前端的结果，两者别混用。
 *
 * <p>取值合法性仍在 Service 层手写校验（项目惯例，不用注解校验，见 PracticeSessionCreateDTO 的说明）。
 */
@Data
public class AiSummaryDTO {

    /** 题库 id（必传）—— Service 会校验题库是否存在 */
    private Long bankId;

    /**
     * 是否强制重新生成：true = 跳过缓存重新调 AI；false/null = 有旧总结就直接返回（fromCache=true）
     *
     * <p>用 Boolean 而不是 boolean：前端不传这个字段时是 null，
     * Service 里按 false 处理（默认走缓存），不会因为缺参直接 400。
     */
    private Boolean force;
}
