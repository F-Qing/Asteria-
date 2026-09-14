package com.asteria.server.Services.impl;

import com.asteria.common.exception.BusinessException;
import com.asteria.pojo.entity.DTO.AnswerSubmitDTO;
import com.asteria.pojo.entity.DTO.PracticeSessionCreateDTO;
import com.asteria.pojo.entity.DTO.PracticeSessionWrongDTO;
import com.asteria.pojo.entity.DTO.StudyStatsDTO;
import com.asteria.pojo.entity.PracticeRecord;
import com.asteria.pojo.entity.PracticeSession;
import com.asteria.pojo.entity.PracticeSessionQuestion;
import com.asteria.pojo.entity.Question;
import com.asteria.pojo.entity.VO.AnswerRecordVO;
import com.asteria.pojo.entity.VO.AnswerSubmitResultVO;
import com.asteria.pojo.entity.VO.ChapterVO;
import com.asteria.pojo.entity.VO.PracticeQuestionVO;
import com.asteria.pojo.entity.VO.PracticeSessionDetailVO;
import com.asteria.pojo.entity.VO.PracticeSessionVO;
import com.asteria.pojo.entity.VO.QuestionOptionVO;
import com.asteria.pojo.entity.VO.RecentSessionVO;
import com.asteria.pojo.entity.VO.SessionResultVO;
import com.asteria.pojo.entity.VO.StudyStatsVO;
import com.asteria.pojo.entity.VO.WrongChapterCountVO;
import com.asteria.pojo.entity.VO.WrongStatsVO;
import com.asteria.pojo.entity.VO.WrongTypeCountVO;
import com.asteria.pojo.entity.WrongQuestion;
import com.asteria.server.Services.PracticeService;
import com.asteria.server.mapper.BankMapper;
import com.asteria.server.mapper.ChapterMapper;
import com.asteria.server.mapper.PracticeRecordMapper;
import com.asteria.server.mapper.PracticeSessionMapper;
import com.asteria.server.mapper.PracticeSessionQuestionMapper;
import com.asteria.server.mapper.QuestionMapper;
import com.asteria.server.mapper.WrongQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 刷题练习 Service 实现。
 *
 * <p>两个创建接口（普通刷题 / 错题重刷）的差别只有「去哪儿抽题」，
 * 落库部分（1 行会话 + N 行题目清单）抽成了共用的 saveSession()，避免两份重复代码。
 *
 * <p>事务说明：@Transactional 标在 public 入口方法上，由 Controller 跨 Bean 调用，
 * 走 Spring 代理所以有效；类内私有方法被入口方法调用时本来就在同一个事务里，
 * 不需要（也不能）再标注解。
 */
@Service
@Slf4j
public class PracticeServiceImpl implements PracticeService {

    private static final String MODE_SEQUENTIAL = "SEQUENTIAL";
    private static final String MODE_RANDOM = "RANDOM";
    private static final String TYPE_ALL = "ALL";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String SOURCE_NORMAL = "NORMAL";
    private static final String SOURCE_WRONG = "WRONG";

    /** 错题本 resolved 字段：0 = 待攻克 / 1 = 已攻克 */
    private static final Integer RESOLVED_NO = 0;
    private static final Integer RESOLVED_YES = 1;

    /** 首页「最近刷题」默认返回条数 / 上限 */
    private static final int DEFAULT_RECENT_LIMIT = 5;
    private static final int MAX_RECENT_LIMIT = 20;

    private static final Set<String> VALID_MODES = Set.of(MODE_SEQUENTIAL, MODE_RANDOM);
    private static final Set<String> VALID_TYPES =
            Set.of("SINGLE", "MULTIPLE", "TRUE_FALSE", "ESSAY", "FILL_BLANK", TYPE_ALL);

    @Autowired
    private BankMapper bankMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private ChapterMapper chapterMapper;
    @Autowired
    private PracticeSessionMapper practiceSessionMapper;
    @Autowired
    private PracticeSessionQuestionMapper practiceSessionQuestionMapper;
    @Autowired
    private PracticeRecordMapper practiceRecordMapper;
    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;
    @Autowired
    private ObjectMapper objectMapper;

    // ============================================================
    // 接口 1：创建刷题会话
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeSessionVO createSession(PracticeSessionCreateDTO dto) {
        // ===== 第 1 步：参数校验 =====
        if (dto == null || dto.getBankId() == null) {
            throw new BusinessException(40010, "bankId 不能为空");
        }
        String mode = normalize(dto.getMode(), MODE_SEQUENTIAL);
        String type = normalize(dto.getQuestionType(), TYPE_ALL);
        checkModeAndType(mode, type);

        // ===== 第 2 步：题库必须存在 =====
        checkBankExists(dto.getBankId());

        // ===== 第 3 步：按条件抽题（题库 + 题型 + 章节）=====
        List<Long> questionIds = pickQuestionIds(dto.getBankId(), type, dto.getChapterId(), mode);
        if (questionIds.isEmpty()) {
            throw new BusinessException(40011, "该条件下没有题目，换个题型或章节试试");
        }

        // ===== 第 4、5、6 步：落库（两个接口共用）=====
        return saveSession(dto.getBankId(), mode, type, dto.getChapterId(), SOURCE_NORMAL, questionIds);
    }

    // ============================================================
    // 接口 2：创建错题重刷会话
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeSessionVO createWrongSession(PracticeSessionWrongDTO dto) {
        // ===== 第 1 步：参数校验（重刷默认随机顺序）=====
        if (dto == null || dto.getBankId() == null) {
            throw new BusinessException(40010, "bankId 不能为空");
        }
        String mode = normalize(dto.getMode(), MODE_RANDOM);
        if (!VALID_MODES.contains(mode)) {
            throw new BusinessException(40010, "mode 只能是 SEQUENTIAL 或 RANDOM，收到：" + mode);
        }

        // ===== 第 2 步：题库必须存在 =====
        checkBankExists(dto.getBankId());

        // ===== 第 3 步：从错题本挑「待攻克」的题 =====
        List<Long> questionIds = pickWrongQuestionIds(dto.getBankId(), mode);
        if (questionIds.isEmpty()) {
            throw new BusinessException(40012, "这个题库还没有待攻克的错题");
        }

        // ===== 第 4、5、6 步：落库 =====
        // 题型固定 ALL、章节固定 null：错题是跨题型、跨章节的，不该再筛
        return saveSession(dto.getBankId(), mode, TYPE_ALL, null, SOURCE_WRONG, questionIds);
    }

    // ============================================================
    // 接口 3：查询会话详情
    // ============================================================
    @Override
    public PracticeSessionDetailVO getSessionDetail(Long sessionId) {
        // ===== 第 1 步：会话本身 =====
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(40402, "刷题会话不存在：" + sessionId);
        }

        PracticeSessionDetailVO vo = new PracticeSessionDetailVO();
        BeanUtils.copyProperties(session, vo);      // 拷 id/bankId/mode/questionType/... 那 9 个字段

        // ===== 第 2 步：题目清单（按出题顺序 sort）=====
        List<PracticeSessionQuestion> items = practiceSessionQuestionMapper.selectList(
                Wrappers.<PracticeSessionQuestion>lambdaQuery()
                        .eq(PracticeSessionQuestion::getSessionId, sessionId)
                        .orderByAsc(PracticeSessionQuestion::getSort));

        List<PracticeQuestionVO> questions = new ArrayList<>(items.size());
        if (!items.isEmpty()) {
            List<Long> questionIds = items.stream()
                    .map(PracticeSessionQuestion::getQuestionId)
                    .collect(Collectors.toList());

            // 章节名不在 question 表里 → 查一次章节建索引（别在循环里查库）
            Map<Long, String> chapterNames = new HashMap<>();
            for (ChapterVO chapter : chapterMapper.selectChaptersWithCount(session.getBankId())) {
                chapterNames.put(chapter.getId(), chapter.getName());
            }

            // ===== 第 3 步：题目详情，一条 IN 查询全捞回来，再按 id 建索引 =====
            Map<Long, Question> questionMap = questionMapper.selectList(
                            Wrappers.<Question>lambdaQuery().in(Question::getId, questionIds))
                    .stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));

            // 按清单的 sort 顺序拼 VO —— IN 查询不保证顺序，所以必须遍历 items 而不是遍历 questionMap
            for (PracticeSessionQuestion item : items) {
                Question q = questionMap.get(item.getQuestionId());
                if (q == null) {
                    continue;               // 外键保证不会发生，防御性跳过
                }
                PracticeQuestionVO qv = new PracticeQuestionVO();
                qv.setId(q.getId());
                qv.setBankId(q.getBankId());
                qv.setChapterId(q.getChapterId());
                qv.setChapterName(chapterNames.getOrDefault(q.getChapterId(), ""));
                qv.setType(q.getType());
                qv.setStem(q.getStem());
                qv.setOptions(parseJson(q.getOptions(), new TypeReference<List<QuestionOptionVO>>() {}));
                qv.setKnowledgePoints(parseJson(q.getKnowledgePoints(), new TypeReference<List<String>>() {}));
                // ⚠️ 这里绝不能拷 answer / analysis —— 防偷看
                questions.add(qv);
            }
        }
        vo.setQuestions(questions);

        // ===== 第 4 步：已答记录 =====
        List<AnswerRecordVO> records = practiceRecordMapper.selectList(
                        Wrappers.<PracticeRecord>lambdaQuery()
                                .eq(PracticeRecord::getSessionId, sessionId)
                                .orderByAsc(PracticeRecord::getId))
                .stream()
                .map(r -> {
                    AnswerRecordVO rv = new AnswerRecordVO();
                    rv.setQuestionId(r.getQuestionId());
                    rv.setUserAnswer(r.getUserAnswer());
                    rv.setIsCorrect(r.getIsCorrect());
                    return rv;
                })
                .collect(Collectors.toList());
        vo.setRecords(records);

        return vo;
    }

    // ============================================================
    // 接口 4：提交单题答案
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnswerSubmitResultVO submitAnswer(Long sessionId, AnswerSubmitDTO dto) {
        // ===== 第 1 步：参数校验 =====
        if (dto == null || dto.getQuestionId() == null) {
            throw new BusinessException(40010, "questionId 不能为空");
        }
        String userAnswer = dto.getUserAnswer() == null ? "" : dto.getUserAnswer().trim();
        if (userAnswer.isEmpty()) {
            throw new BusinessException(40010, "答案不能为空");
        }

        // ===== 第 2 步：会话、题目、归属校验 =====
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(40402, "刷题会话不存在：" + sessionId);
        }
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new BusinessException(40403, "题目不存在：" + dto.getQuestionId());
        }
        // 这题必须在本次会话的题目清单里，否则进度会被搞乱（拿别的会话的题 id 乱提交）
        Long belong = practiceSessionQuestionMapper.selectCount(
                Wrappers.<PracticeSessionQuestion>lambdaQuery()
                        .eq(PracticeSessionQuestion::getSessionId, sessionId)
                        .eq(PracticeSessionQuestion::getQuestionId, dto.getQuestionId()));
        if (belong == null || belong == 0) {
            throw new BusinessException(40013, "这道题不属于本次会话：" + dto.getQuestionId());
        }

        // ===== 第 3 步：判题 =====
        Boolean isCorrect = judge(question, userAnswer);

        // ===== 第 4 步：写答题记录（同题重答 = 覆盖那一行）=====
        saveRecord(sessionId, dto.getQuestionId(), userAnswer, isCorrect);

        // ===== 第 5 步：维护错题本 =====
        updateWrongBook(session.getBankId(), dto.getQuestionId(), userAnswer, isCorrect);

        // ===== 第 6 步：重算进度 + 判断是否做完 =====
        practiceSessionMapper.refreshCounts(sessionId);
        PracticeSession latest = practiceSessionMapper.selectById(sessionId);
        if (latest.getAnsweredCount() >= latest.getTotalCount()
                && !STATUS_COMPLETED.equals(latest.getStatus())) {
            PracticeSession patch = new PracticeSession();
            patch.setId(sessionId);
            patch.setStatus(STATUS_COMPLETED);
            patch.setCompletedAt(LocalDateTime.now());
            practiceSessionMapper.updateById(patch);
            latest.setStatus(STATUS_COMPLETED);
            log.info("会话已全部答完：sessionId={}, 共 {} 题", sessionId, latest.getTotalCount());
        }

        log.info("提交答案：sessionId={}, questionId={}, isCorrect={}, 进度 {}/{}",
                sessionId, dto.getQuestionId(), isCorrect,
                latest.getAnsweredCount(), latest.getTotalCount());

        // ===== 第 7 步：组装返回（这一步才把答案和解析给前端）=====
        AnswerSubmitResultVO vo = new AnswerSubmitResultVO();
        vo.setQuestionId(dto.getQuestionId());
        vo.setUserAnswer(userAnswer);
        vo.setIsCorrect(isCorrect);
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setAnsweredCount(latest.getAnsweredCount());
        vo.setCorrectCount(latest.getCorrectCount());
        vo.setSessionStatus(latest.getStatus());
        return vo;
    }

    // ============================================================
    // 接口 5：查询会话结果
    // ============================================================
    @Override
    public SessionResultVO getSessionResult(Long sessionId) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(40402, "刷题会话不存在：" + sessionId);
        }

        SessionResultVO vo = new SessionResultVO();
        vo.setSessionId(sessionId);
        vo.setTotalCount(session.getTotalCount());
        vo.setAnsweredCount(session.getAnsweredCount());
        vo.setCorrectCount(session.getCorrectCount());
        vo.setAccuracy(calcAccuracy(session.getAnsweredCount(), session.getCorrectCount()));

        // 按题型统计 + 错题明细，各一条 SQL（都不随题数增长）
        vo.setTypeStats(practiceSessionQuestionMapper.countByType(sessionId));
        vo.setWrongQuestions(practiceRecordMapper.selectWrongItems(sessionId));
        return vo;
    }

    // ============================================================
    // 接口 6：查询错题统计
    // ============================================================
    @Override
    public WrongStatsVO getWrongStats(Long bankId) {
        if (bankId == null) {
            throw new BusinessException(40010, "bankId 不能为空");
        }
        checkBankExists(bankId);

        // 按题型、按章节各一条 GROUP BY（都不随错题数增长）
        List<WrongTypeCountVO> byType = wrongQuestionMapper.countGroupByType(bankId);
        List<WrongChapterCountVO> byChapter = wrongQuestionMapper.countGroupByChapter(bankId);

        WrongStatsVO vo = new WrongStatsVO();
        vo.setBankId(bankId);
        // 总数直接用 byType 求和，不再单独 COUNT 一次：
        // 这样"总数"和"各题型之和"永远相等，前端不会出现"总数 7 但标签加起来 6"的自相矛盾
        vo.setWrongTotal(byType.stream()
                .mapToInt(t -> t.getCount() == null ? 0 : t.getCount())
                .sum());
        vo.setByType(byType);
        vo.setByChapter(byChapter);
        return vo;
    }

    // ============================================================
    // 接口 7：查询学习统计（首页统计卡）
    // ============================================================
    @Override
    public StudyStatsVO getStudyStats(Long bankId) {
        // bankId 可选：传了才校验题库存在，不传就是统计全部题库
        if (bankId != null) {
            checkBankExists(bankId);
        }

        StudyStatsDTO raw = practiceSessionMapper.selectStudyStats(bankId);
        int practiced = (raw == null || raw.getPracticed() == null) ? 0 : raw.getPracticed();
        int correct = (raw == null || raw.getCorrect() == null) ? 0 : raw.getCorrect();

        StudyStatsVO vo = new StudyStatsVO();
        vo.setPracticed(practiced);
        // 复用会话结果那套算法：分母是"已答"，没答过返回 0（顺带挡掉除零）
        vo.setAccuracy(calcAccuracy(practiced, correct));
        return vo;
    }

    // ============================================================
    // 接口 8：查询最近刷题列表（首页「最近刷题」卡片）
    // ============================================================
    @Override
    public List<RecentSessionVO> listRecentSessions(Integer limit) {
        int size = (limit == null || limit < 1) ? DEFAULT_RECENT_LIMIT
                                                : Math.min(limit, MAX_RECENT_LIMIT);
        return practiceSessionMapper.selectRecent(size);
    }

    // ============================================================
    // 判题
    // ============================================================

    /**
     * 判题：true 对 / false 错 / null 不判分（简答题）。
     *
     * <p>题库导入时已经按契约统一过答案格式，所以这里只做保守比对：
     * 单选/判断全等，多选忽略顺序，填空逐空比。
     */
    private Boolean judge(Question question, String userAnswer) {
        String std = normalizeText(question.getAnswer());
        String mine = normalizeText(userAnswer);
        return switch (question.getType()) {
            case "SINGLE", "TRUE_FALSE" -> std.equals(mine);
            case "MULTIPLE" -> letters(std).equals(letters(mine));   // ACD == DCA
            case "FILL_BLANK" -> fillBlankEquals(std, mine);
            case "ESSAY" -> null;            // 简答题没有 AI 判分，不下结论（前端按"自评"处理）
            default -> std.equals(mine);     // 万一以后加新题型，保守按全等
        };
    }

    /** 去首尾空格 + 转大写；null 当空串，避免 NPE */
    private String normalizeText(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    /** 多选答案规整：只保留字母再排序（"A,C,D" / "DCA" / "acd" 都变成 "ACD"） */
    private String letters(String s) {
        char[] cs = s.replaceAll("[^A-Z]", "").toCharArray();
        Arrays.sort(cs);
        return new String(cs);
    }

    /** 填空：多空按「；」分隔（兼容半角 ;），个数和每一空都要对上 */
    private boolean fillBlankEquals(String std, String mine) {
        String[] stdParts = std.split("[；;]");
        String[] myParts = mine.split("[；;]");
        if (stdParts.length != myParts.length) {
            return false;
        }
        for (int i = 0; i < stdParts.length; i++) {
            if (!stdParts[i].trim().equals(myParts[i].trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 正确率 0~100（整数）。
     *
     * <p>分母用「已答题数」而不是「总题数」：没答的题不该拉低正确率。
     * 一题都没答时返回 0，顺手把除零挡掉。
     *
     * <p>会话结果接口和学习统计接口都用这一个方法 —— 口径只有一份，
     * 不会出现首页显示 78% 而完成页显示 80% 的情况。
     *
     * <p>注意：简答题 is_correct 是 NULL，算"已答"但不算"答对"，
     * 所以卷子里有简答题时正确率天然到不了 100%（这是之前确认过的行为）。
     */
    private Integer calcAccuracy(Integer answeredCount, Integer correctCount) {
        int answered = answeredCount == null ? 0 : answeredCount;
        int correct = correctCount == null ? 0 : correctCount;
        if (answered <= 0) {
            return 0;
        }
        return Math.round(correct * 100f / answered);
    }

    // ============================================================
    // 写答题记录 / 维护错题本
    // ============================================================

    /** 写答题记录：没答过就插入，答过就覆盖（靠 uk_pr_session_question 唯一键兜底） */
    private void saveRecord(Long sessionId, Long questionId, String userAnswer, Boolean isCorrect) {
        PracticeRecord record = practiceRecordMapper.selectOne(
                Wrappers.<PracticeRecord>lambdaQuery()
                        .eq(PracticeRecord::getSessionId, sessionId)
                        .eq(PracticeRecord::getQuestionId, questionId));
        if (record == null) {
            PracticeRecord fresh = new PracticeRecord();
            fresh.setSessionId(sessionId);
            fresh.setQuestionId(questionId);
            fresh.setUserAnswer(userAnswer);
            fresh.setIsCorrect(isCorrect);
            practiceRecordMapper.insert(fresh);
        } else {
            record.setUserAnswer(userAnswer);
            record.setIsCorrect(isCorrect);
            practiceRecordMapper.updateById(record);
        }
    }

    /**
     * 错题本维护：
     * <ul>
     *   <li>答错 → 没收录就插入，已收录就答错次数 +1、并把"已攻克"打回"待攻克"</li>
     *   <li>答对 → 如果这题在错题本里，标记为"已攻克"（不再统计、不再被重刷选中）</li>
     *   <li>不判分（简答题）→ 什么都不做</li>
     * </ul>
     */
    private void updateWrongBook(Long bankId, Long questionId, String userAnswer, Boolean isCorrect) {
        if (isCorrect == null) {
            return;     // 没判分的题不碰错题本
        }
        WrongQuestion existing = wrongQuestionMapper.selectOne(
                Wrappers.<WrongQuestion>lambdaQuery()
                        .eq(WrongQuestion::getBankId, bankId)
                        .eq(WrongQuestion::getQuestionId, questionId));

        if (Boolean.TRUE.equals(isCorrect)) {
            if (existing != null && !RESOLVED_YES.equals(existing.getResolved())) {
                existing.setResolved(RESOLVED_YES);
                wrongQuestionMapper.updateById(existing);
            }
            return;
        }

        if (existing == null) {
            WrongQuestion wq = new WrongQuestion();
            wq.setBankId(bankId);
            wq.setQuestionId(questionId);
            wq.setUserAnswer(userAnswer);
            wq.setWrongCount(1);
            wq.setResolved(RESOLVED_NO);
            wrongQuestionMapper.insert(wq);
        } else {
            existing.setUserAnswer(userAnswer);
            existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
            existing.setResolved(RESOLVED_NO);      // 攻克过的题又错了，打回"待攻克"
            wrongQuestionMapper.updateById(existing);
        }
    }

    // ============================================================
    // 抽题
    // ============================================================

    /**
     * 普通抽题：按 题库 / 题型 / 章节 查题目 id。
     *
     * <p>顺序模式按题目 id 升序；随机模式在内存里打乱；不做数量截断。
     */
    private List<Long> pickQuestionIds(Long bankId, String type, Long chapterId, String mode) {
        LambdaQueryWrapper<Question> wrapper = Wrappers.<Question>lambdaQuery()
                .select(Question::getId)                                 // 只查 id：题干、options JSON 都是大字段
                .eq(Question::getBankId, bankId)
                .eq(!TYPE_ALL.equals(type), Question::getType, type)      // 条件开关：ALL 就不拼
                .eq(chapterId != null, Question::getChapterId, chapterId) // null 就不拼
                .orderByAsc(Question::getId);

        List<Long> ids = questionMapper.selectList(wrapper).stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        if (MODE_RANDOM.equals(mode)) {
            Collections.shuffle(ids);        // Java 里打乱，不用 SQL 的 ORDER BY RAND()
        }
        return ids;
    }

    /**
     * 错题抽题：只挑 resolved = 0（待攻克）的。
     *
     * <p>顺序模式按错题本 id 升序 —— 错题本的时间字段已经删了，
     * id 是唯一能反映"收录先后"的东西（先错的排前面）。
     * 想改成"错得最多的先刷"，把 orderByAsc 换成 {@code orderByDesc(WrongQuestion::getWrongCount)}。
     */
    private List<Long> pickWrongQuestionIds(Long bankId, String mode) {
        List<Long> ids = wrongQuestionMapper.selectList(
                        Wrappers.<WrongQuestion>lambdaQuery()
                                .select(WrongQuestion::getQuestionId)
                                .eq(WrongQuestion::getBankId, bankId)
                                .eq(WrongQuestion::getResolved, RESOLVED_NO)
                                .orderByAsc(WrongQuestion::getId))
                .stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toList());

        if (MODE_RANDOM.equals(mode)) {
            Collections.shuffle(ids);
        }
        return ids;
    }

    // ============================================================
    // 共用：落库（写 1 行会话 + N 行题目清单）
    // ============================================================

    /**
     * 两个创建接口共用的落库逻辑。
     *
     * <p>注意这里是类内私有方法，被两个 public 入口调用时已经在事务里了，
     * 所以不需要再标 @Transactional（同类自调用标了也不会生效）。
     */
    private PracticeSessionVO saveSession(Long bankId, String mode, String type, Long chapterId,
                                          String source, List<Long> questionIds) {
        // ===== 写会话（practice_session，1 行）=====
        PracticeSession session = new PracticeSession();
        session.setBankId(bankId);
        session.setMode(mode);
        session.setQuestionType(type);
        session.setChapterId(chapterId);            // null = 全章节
        session.setStatus(STATUS_IN_PROGRESS);
        session.setTotalCount(questionIds.size());
        session.setAnsweredCount(0);
        session.setCorrectCount(0);
        session.setSessionSource(source);           // NORMAL / WRONG
        practiceSessionMapper.insert(session);      // 自增主键回填到 session.id

        log.info("创建刷题会话成功：sessionId={}, bankId={}, source={}, mode={}, type={}, chapterId={}, 共 {} 题",
                session.getId(), bankId, source, mode, type, chapterId, questionIds.size());

        // ===== 写题目清单（practice_session_question，N 行）=====
        List<PracticeSessionQuestion> items = new ArrayList<>(questionIds.size());
        for (int i = 0; i < questionIds.size(); i++) {
            // 每行必须 new 新对象，不能复用（题库导入那边踩过：整张表变成同一道题）
            PracticeSessionQuestion item = new PracticeSessionQuestion();
            item.setSessionId(session.getId());
            item.setQuestionId(questionIds.get(i));
            item.setSort(i + 1);                    // 从 1 开始
            items.add(item);
        }
        practiceSessionQuestionMapper.insertBatch(items);

        // ===== 实体 → VO =====
        PracticeSessionVO vo = new PracticeSessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    // ============================================================
    // 小工具
    // ============================================================

    /**
     * JSON 字符串 → 对象/列表；空值或解析失败都返回 null。
     *
     * <p>不用 MyBatis-Plus 的 JacksonTypeHandler 自动转换，是因为那要求实体上
     * 额外加 @TableName(autoResultMap = true)，漏了就静默失效（查出来是 null 还不报错）。
     * 手工转换麻烦一点，但行为完全可控。
     *
     * <p>解析失败也返回 null 而不是抛异常：库里可能有脏数据，
     * 不能因为一道题的 options 格式不对就让整个"会话详情"接口 500。
     */
    private <T> T parseJson(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("JSON 解析失败：{}", json, e);
            return null;
        }
    }

    /** mode / questionType 取值校验，两个创建接口共用 */
    private void checkModeAndType(String mode, String type) {
        if (!VALID_MODES.contains(mode)) {
            throw new BusinessException(40010, "mode 只能是 SEQUENTIAL 或 RANDOM，收到：" + mode);
        }
        if (!VALID_TYPES.contains(type)) {
            throw new BusinessException(40010, "questionType 取值不合法：" + type);
        }
    }

    /** 题库存在性校验：外键也会拦，但报的是 errno 1452，前端看不懂 */
    private void checkBankExists(Long bankId) {
        if (bankMapper.selectById(bankId) == null) {
            throw new BusinessException(40401, "题库不存在：" + bankId);
        }
    }

    /** 空值兜底 + 去空格 + 转大写 */
    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
