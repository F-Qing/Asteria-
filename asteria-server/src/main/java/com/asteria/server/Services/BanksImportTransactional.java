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
     * <p>顺序：插题库 → 插默认章节 → 逐题（判型/归一化/过滤/插入）→ 更新任务为 SUCCESS。
     * 任何一步抛异常 → 整个事务回滚，不会留下"半截题库"。
     */
    @Transactional(rollbackFor = Exception.class)
    public Outcome saveImport(String taskId, String bankName, String originalFileName, String ext,
                              List<RawQuestion> rawQuestions) throws JsonProcessingException {

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

            // 3.2 答案取不到 → 也跳过（否则会往 NOT NULL 的 answer 列插 NULL，整个批次报错）
            String answer = answerNormalizer.normalize(rawQuestion, type);
            if (answer == null || answer.isBlank()) {
                skipped++;
                skippedNos.add(no);
                continue;
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
            throw new IllegalStateException("文件中没有可入库的题目（共 " + no + " 题，全部被跳过）");
        }

        // 4) 任务收尾：必须放在所有题目插入【之后】，且和上面同一个事务
        bankImportMapper.updateById(BankImport.builder()
                .taskId(taskId)
                .status(ImportStatus.SUCCESS.name())
                .progress(100)
                .totalCount(inserted)
                .bankId(bankId)
                .build());

        if (skipped > 0) {
            log.warn("taskId={} 跳过 {} 题（第 {} 题）：题型无法识别或答案缺失",
                    taskId, skipped, skippedNos);
        }
        log.info("入库完成：taskId={}, bankId={}, 入库{}题, 跳过{}题",
                taskId, bankId, inserted, skipped);

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
