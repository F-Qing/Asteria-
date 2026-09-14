package com.asteria.server.controller;

import com.asteria.common.result.ApiResponse;
import com.asteria.pojo.entity.QuestionPage;
import com.asteria.server.Services.QuestionsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/banks")
public class QuestionsController {
    @Autowired
    private QuestionsService questionsService;

    /**
     * 题目分页查询
     * GET /api/banks/{bankId}/questions?chapterId=&type=&keyword=&page=1&pageSize=20
     *
     * <p>chapterId / type / keyword 三个筛选参数都可选，前端不传就查整库
     * （参数清单见《Asteria AI 接口文档》「题目分页查询」）。
     */
    @GetMapping("/{bankId}/questions")
    public ApiResponse<QuestionPage> getQuestions(@PathVariable Long bankId,
                                                 @RequestParam(value = "chapterId", required = false) Long chapterId,
                                                 @RequestParam(value = "type", required = false) String type,
                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                 @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        log.info("getQuestions: bankId={}, chapterId={}, type={}, keyword={}, page={}, pageSize={}",
                bankId, chapterId, type, keyword, page, pageSize);
        QuestionPage questionPage = questionsService.PageQuestions(bankId, chapterId, type, keyword, page, pageSize);
        return ApiResponse.ok(questionPage);
    }

}
