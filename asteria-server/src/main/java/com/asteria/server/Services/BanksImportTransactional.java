package com.asteria.server.Services;

import com.asteria.common.Tool.RawQuestion;
import com.asteria.pojo.entity.Bank;
import com.asteria.pojo.entity.BankImport;
import com.asteria.pojo.entity.Chapter;
import com.asteria.pojo.entity.Question;
import com.asteria.pojo.enums.ImportStatus;
import com.asteria.pojo.enums.QuestionType;
import com.asteria.server.mapper.BankMapper;
import com.asteria.server.mapper.BanksImportMapper;
import com.asteria.server.mapper.ChapterMapper;
import com.asteria.server.mapper.QuestionMapper;
import com.asteria.server.parser.AnswerNormalizer;
import com.asteria.server.parser.QuestionClassifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入的「入库」环节：题库 + 章节 + 题目 + 任务状态，全部在**一个事务**里。
 *
 * <p>为什么要单独一个类：Spring 的 {@code @Transactional} 靠代理生效，
 * 同类内部调用（this.xxx）不走代理。把它放在独立 Bean 上、由后台线程**跨 Bean 调用**，
 * 事务才真的生效。
 *
 * <p>这里只做"写库"。解析、判型、归一化、读写文件、sleep 都在事务外面。
 */
@Service
@Slf4j
public class BanksImportTransactional {

    /** error_message 列宽上限（varchar(500)） */
    private static final int MAX_ERROR_LENGTH = 500;

    @Autowired
    private BankMapper bankMapper;
    @Autowired
    private BanksImportMapper bankImportMapper;
    @Autowired
    private ChapterMapper chapterMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionClassifier questionClassifier;
    @Autowired
    private AnswerNormalizer answerNormalizer;
    @Autowired
    private ObjectMapper objectMapper;

    /** 一次导入的结果：题库 id、实际入库题数、跳过的题数 */
    public record Outcome(Long bankId, int totalCount, int skippedCount) {
    }

    /**
     * 一次导入的入库动作（事务方法）。
     *
     * <p>顺序：插题库 → 插默认章节 → 逐题（判型/归一化/过滤/插入）→ 更新任务状态。
     * 任何一步抛异常 → 整个事务回滚，不会留下"半截题库"。
     *
     * @param aiParse 是否还要做 AI 解析：true 时任务状态停在 AI_PROCESSING，
     *                由后台的 AI 阶段跑完再收尾成 SUCCESS
     */
    @Transactional(rollbackFor = Exception.class)
    public Outcome saveImport(String taskId, String bankName, String originalFileName, String ext,
                              List<RawQuestion> rawQuestions, boolean aiParse) throws JsonProcessingException {

        // 1) 题库
        Bank bank = new Bank();
        bank.setName(bankName);
        bank.setFileName(originalFileName);
        bank.setFileType(ext);
        bankMapper.insert(bank);
        Long bankId = bank.getId();

        // 2) 默认章节（文件里没有章节信息时，所有题都挂这一章）
        Chapter chapter = new Chapter();
        chapter.setBankId(bankId);
        chapter.setName("默认章节");
        chapter.setSort(1);
        chapterMapper.insert(chapter);
        Long chapterId = chapter.getId();

        // 3) 逐题：判型 → 归一化 → 过滤 → 插入
        int no = 0;
        int inserted = 0;
        int skipped = 0;
        List<Integer> skippedNos = new ArrayList<>();

        for (RawQuestion rawQuestion : rawQuestions) {
            no++;

            // 3.1 判不出题型 → 跳过（不入库）
            QuestionType type = questionClassifier.classify(rawQuestion);
            if (type == null) {
                skipped++;
                skippedNos.add(no);
                continue;
            }

            // 3.2 【改】答案取不到不再跳过：answer 列是 NOT NULL，用空串占位，
            //     缺答案的题照样入库，交给后台的 AI 阶段补答案（未开启 AI 时就一直是空串）
            String answer = answerNormalizer.normalize(rawQuestion, type);
            if (answer == null) {
                answer = "";
            }

            // 3.3 组装并插入（注意：每题都 new 一个新对象，绝不复用同一个）
            Question question = new Question();
            question.setBankId(bankId);
            question.setChapterId(chapterId);
            question.setType(type.name());
            question.setStem(rawQuestion.getRawStem());
            question.setOptions(objectMapper.writeValueAsString(rawQuestion.getRawOptions()));
            question.setAnswer(answer);
            questionMapper.insert(question);
            inserted++;
        }

        // 3.4 一道题都没入库 → 整体失败（抛异常让事务回滚，避免留下一个空题库）
        if (inserted == 0) {
            // 两种情况的成因完全不同，提示要分开给，用户才知道该改什么：
            //   no == 0 → 连题都没切出来，多半是缺【第 N 题】题号（没有题号整份文件都进不了切块）
            //   no  > 0 → 切出来了但题型全判不出，多半是缺题型/缺答案
            throw new IllegalStateException(no == 0
                    ? "未识别到题目，请按标准格式整理：每道题以【第 N 题】开头，并写「题目：题干」（上传页可查看格式要求）"
                    : "解析出 " + no + " 题但全部无法入库（题型无法识别），请按标准格式整理后重新上传（上传页可查看格式要求）");
        }

        // 4) 任务收尾：必须放在所有题目插入【之后】，且和上面同一个事务。
        //    【改】要 AI 解析就先别标 SUCCESS —— 否则前端轮询到 SUCCESS 就停止轮询、
        //    刷新题库了，而 AI 还一道题都没跑。最终状态由后面的 AI 阶段收尾。
        bankImportMapper.updateById(BankImport.builder()
                .taskId(taskId)
                .status(aiParse ? ImportStatus.AI_PROCESSING.name() : ImportStatus.SUCCESS.name())
                .progress(aiParse ? 0 : 100)
                .totalCount(inserted)
                .bankId(bankId)
                .build());

        if (skipped > 0) {
            log.warn("taskId={} 跳过 {} 题（题型无法识别）：第 {} 题", taskId, skipped, skippedNos);
        }
        log.info("入库完成：taskId={}, bankId={}, 入库{}题, 跳过{}题, aiParse={}",
                taskId, bankId, inserted, skipped, aiParse);

        return new Outcome(bankId, inserted, skipped);
    }

    /**
     * 记录导入失败（**独立事务**）。
     *
     * <p>必须由调用方在 catch 里调用：如果把它写在入库事务里，事务回滚会把"失败记录"一起抹掉，
     * 任务就会永远卡在 PARSING。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(String taskId, String reason) {
        bankImportMapper.updateById(BankImport.builder()
                .taskId(taskId)
                .status(ImportStatus.FAILED.name())
                .errorMessage(truncate(reason))
                .build());
    }

    /** 截断到列宽以内（error_message 是 varchar(500)，而异常信息可能上千字符） */
    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}
