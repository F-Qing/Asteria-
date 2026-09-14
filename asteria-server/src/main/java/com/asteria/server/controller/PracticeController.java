package com.asteria.server.controller;

import com.asteria.common.result.ApiResponse;
import com.asteria.pojo.entity.DTO.AnswerSubmitDTO;
import com.asteria.pojo.entity.DTO.PracticeSessionCreateDTO;
import com.asteria.pojo.entity.DTO.PracticeSessionWrongDTO;
import com.asteria.pojo.entity.VO.AnswerSubmitResultVO;
import com.asteria.pojo.entity.VO.PracticeSessionDetailVO;
import com.asteria.pojo.entity.VO.PracticeSessionVO;
import com.asteria.pojo.entity.VO.RecentSessionVO;
import com.asteria.pojo.entity.VO.SessionResultVO;
import com.asteria.pojo.entity.VO.StudyStatsVO;
import com.asteria.pojo.entity.VO.WrongStatsVO;
import com.asteria.server.Services.PracticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 刷题练习接口，统一前缀 /api/practice。
 *
 * <p>前 6 个来自接口文档「刷题练习接口」；后 2 个（学习统计 / 最近刷题列表）
 * 是首页要用的，接口文档里已补上。
 *
 * <p>校验失败的返回不用在这里写：Service 抛 BusinessException，
 * 由 GlobalExceptionHandler 统一翻译成 { code, message }。
 */
@RestController
@Slf4j
@RequestMapping("/api/practice")
public class PracticeController {

    @Autowired
    private PracticeService practiceService;

    /**
     * 创建刷题会话：POST /api/practice/sessions
     * Body: { "bankId": 20, "mode": "SEQUENTIAL", "questionType": "SINGLE", "chapterId": null }
     */
    @PostMapping("/sessions")
    public ApiResponse<PracticeSessionVO> createSession(@RequestBody PracticeSessionCreateDTO dto) {
        log.info("创建刷题会话：{}", dto);
        return ApiResponse.ok(practiceService.createSession(dto));
    }

    /**
     * 创建错题重刷会话：POST /api/practice/sessions/wrong
     * Body: { "bankId": 20, "mode": "RANDOM" }
     *
     * <p>路径 /sessions/wrong 和 /sessions 是两个不同路径，不会冲突。
     */
    @PostMapping("/sessions/wrong")
    public ApiResponse<PracticeSessionVO> createWrongSession(@RequestBody PracticeSessionWrongDTO dto) {
        log.info("创建错题重刷会话：{}", dto);
        return ApiResponse.ok(practiceService.createWrongSession(dto));
    }

    /**
     * 查询会话详情：GET /api/practice/sessions/{id}
     *
     * <p>这是 GET，和上面两个 POST 不冲突（方法不同）。
     */
    @GetMapping("/sessions/{id}")
    public ApiResponse<PracticeSessionDetailVO> getSessionDetail(@PathVariable Long id) {
        log.info("查询刷题会话详情：sessionId={}", id);
        return ApiResponse.ok(practiceService.getSessionDetail(id));
    }

    /**
     * 提交单题答案：POST /api/practice/sessions/{id}/answers
     * Body: { "questionId": 402, "userAnswer": "B" }
     *
     * <p>返回里才带标准答案和解析 —— 会话详情刻意不给（防偷看）。
     */
    @PostMapping("/sessions/{id}/answers")
    public ApiResponse<AnswerSubmitResultVO> submitAnswer(@PathVariable Long id,
                                                         @RequestBody AnswerSubmitDTO dto) {
        log.info("提交单题答案：sessionId={}, dto={}", id, dto);
        return ApiResponse.ok(practiceService.submitAnswer(id, dto));
    }

    /**
     * 查询会话结果：GET /api/practice/sessions/{id}/result
     *
     * <p>路径比详情多一段 /result，和 GET /sessions/{id} 不冲突。
     */
    @GetMapping("/sessions/{id}/result")
    public ApiResponse<SessionResultVO> getSessionResult(@PathVariable Long id) {
        log.info("查询会话结果：sessionId={}", id);
        return ApiResponse.ok(practiceService.getSessionResult(id));
    }

    /**
     * 查询错题统计：GET /api/practice/wrong-stats?bankId=20
     *
     * <p>bankId 是 Query 参数（不是路径参数），和接口文档一致。
     * 写成 required = false 是故意的：缺参数时让 Service 抛 40010 给出人能看懂的提示，
     * 否则 Spring 会在进 Service 之前就拒绝请求，最后走到全局兜底变成 500。
     */
    @GetMapping("/wrong-stats")
    public ApiResponse<WrongStatsVO> getWrongStats(
            @RequestParam(value = "bankId", required = false) Long bankId) {
        log.info("查询错题统计：bankId={}", bankId);
        return ApiResponse.ok(practiceService.getWrongStats(bankId));
    }

    /**
     * 查询学习统计：GET /api/practice/stats?bankId=20
     *
     * <p>bankId 可选（不传 = 全部题库），所以 required = false。
     */
    @GetMapping("/stats")
    public ApiResponse<StudyStatsVO> getStudyStats(
            @RequestParam(value = "bankId", required = false) Long bankId) {
        log.info("查询学习统计：bankId={}", bankId);
        return ApiResponse.ok(practiceService.getStudyStats(bankId));
    }

    /**
     * 查询最近刷题列表：GET /api/practice/sessions?limit=5
     *
     * <p>注意这里是 GET /sessions（列表），和已有的 GET /sessions/{id}（详情）
     * 是两个不同路径，Spring 不会冲突。
     */
    @GetMapping("/sessions")
    public ApiResponse<List<RecentSessionVO>> listRecentSessions(
            @RequestParam(value = "limit", required = false) Integer limit) {
        log.info("查询最近刷题列表：limit={}", limit);
        return ApiResponse.ok(practiceService.listRecentSessions(limit));
    }
}
