package com.asteria.server.Services;

import com.asteria.pojo.entity.QuestionPage;

public interface QuestionsService {

    /**
     * 题目分页查询（浏览 / 检索）
     *
     * <p>参数与《Asteria AI 接口文档》「题目分页查询」一致：
     *
     * @param bankId    题库 id（路径参数，必有）
     * @param chapterId 章节 id；null = 不按章节筛（看整库）
     * @param type      题型 SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK；null 或空串 = 不按题型筛
     * @param keyword   题干关键字（模糊匹配）；null 或空串 = 不搜
     * @param page      页码，从 1 开始；null 或 &lt;1 兜底为 1
     * @param pageSize  每页条数；null 或 &lt;1 兜底为 20，上限 100
     */
    QuestionPage PageQuestions(Long bankId, Long chapterId, String type, String keyword,
                               Integer page, Integer pageSize);
}
