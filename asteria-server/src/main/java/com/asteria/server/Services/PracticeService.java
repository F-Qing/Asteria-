package com.asteria.server.Services;

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

import java.util.List;

/**
 * 刷题练习 Service。
 *
 * <p>接口文档「刷题练习接口」里的 6 个（创建会话 / 错题重刷 / 会话详情 / 提交答案 /
 * 会话结果 / 错题统计）已全部完成；后两个（学习统计 / 最近刷题列表）
 * 是首页要用的，属于文档外新增、已补进接口文档。
 */
public interface PracticeService {

    /**
     * 创建刷题会话：按 题库 + 题型 + 章节 抽题，落一条会话 + 一份题目清单。
     *
     * @param dto 前端传来的 { bankId, mode, questionType, chapterId }
     * @return 新建的会话（含自增 id，前端拿它跳转刷题页）
     */
    PracticeSessionVO createSession(PracticeSessionCreateDTO dto);

    /**
     * 创建错题重刷会话：把这个题库里「待攻克」（resolved=0）的错题抽出来组一次会话。
     *
     * <p>题型固定 ALL、章节固定全章节 —— 错题是跨题型、跨章节的，不该再筛。
     *
     * @param dto { bankId, mode }，mode 缺省 RANDOM
     * @return 新建的会话（结构同创建刷题会话）
     */
    PracticeSessionVO createWrongSession(PracticeSessionWrongDTO dto);

    /**
     * 查询会话详情：会话进度 + 题目列表（不含答案，防偷看）+ 已答记录。
     *
     * @param sessionId 会话 id
     * @return 会话详情
     * @throws com.asteria.common.exception.BusinessException 会话不存在时抛 40402
     */
    PracticeSessionDetailVO getSessionDetail(Long sessionId);

    /**
     * 提交单题答案：判题 → 写答题记录 → 维护错题本 → 重算会话进度（可能标记完成）。
     *
     * @param sessionId 会话 id（路径参数）
     * @param dto       { questionId, userAnswer }
     * @return 判题结果（含标准答案与解析、更新后的进度）
     * @throws com.asteria.common.exception.BusinessException 会话/题目不存在，或这道题不属于本次会话
     */
    AnswerSubmitResultVO submitAnswer(Long sessionId, AnswerSubmitDTO dto);

    /**
     * 查询会话结果（完成页）：总分 + 按题型统计 + 错题明细。
     *
     * <p>会话没答完也能查，返回当前进度（前端可能中途想看）。
     *
     * @param sessionId 会话 id
     * @return 会话结果；accuracy 单位是 0~100
     * @throws com.asteria.common.exception.BusinessException 会话不存在时抛 40402
     */
    SessionResultVO getSessionResult(Long sessionId);

    /**
     * 查询错题统计：这个题库里「待攻克」（resolved=0）的错题总数 + 按题型/章节分布。
     *
     * <p>跨会话累计，跟具体某次刷题无关 —— 数据源是错题本表，不是会话表。
     *
     * @param bankId 题库 id（必传）
     * @return 错题统计
     * @throws com.asteria.common.exception.BusinessException bankId 为空或题库不存在
     */
    WrongStatsVO getWrongStats(Long bankId);

    /**
     * 查询学习统计（首页统计卡）：累计作答次数 + 总体正确率。
     *
     * @param bankId 题库 id；传 null = 统计全部题库
     * @return practiced 累计作答次数、accuracy 0~100
     * @throws com.asteria.common.exception.BusinessException 传了 bankId 但题库不存在
     */
    StudyStatsVO getStudyStats(Long bankId);

    /**
     * 查询最近刷题列表（首页「最近刷题」卡片）：按创建时间倒序。
     *
     * @param limit 返回条数；null/小于 1 按 5，超过 20 按 20
     * @return 会话摘要列表（含题库名）
     */
    List<RecentSessionVO> listRecentSessions(Integer limit);
}
