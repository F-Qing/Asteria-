package com.asteria.server.mapper;

import com.asteria.pojo.entity.PracticeSessionQuestion;
import com.asteria.pojo.entity.VO.TypeStatVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 会话题目清单 Mapper（表 practice_session_question） */
@Mapper
public interface PracticeSessionQuestionMapper extends BaseMapper<PracticeSessionQuestion> {

    /**
     * 批量插入会话题目清单：一条 SQL 插 N 行。
     *
     * <p>为什么不用循环调 insert()：一次会话可能几十上百题，
     * 循环就是上百次「应用 → 数据库」往返，慢不说，还会把 MyBatis 日志刷满。
     *
     * <p>注意 collection='list' 要和 @Param("list") 对上，不然 MyBatis 找不到参数。
     */
    @Insert("""
            <script>
            INSERT INTO practice_session_question (session_id, question_id, sort) VALUES
            <foreach collection="list" item="item" separator=",">
                (#{item.sessionId}, #{item.questionId}, #{item.sort})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<PracticeSessionQuestion> list);

    /**
     * 按题型统计本次会话：每个题型共几题、答对几题。
     *
     * <p>以 practice_session_question 为基准（不是 question 表）——
     * 一次会话可能只考了题库里的一部分题，从 question 出发会把没考的题也算进来。
     *
     * <p>LEFT JOIN 答题记录：没答的题 total 也算进去，correct 记 0。
     * 用 SUM(CASE WHEN ...) 而不是 SUM(r.is_correct = 1)：后者在整行是 NULL 时会得到 NULL，
     * 映射到 Java 就是 null，还得判空；CASE WHEN 保证永远是数字。
     */
    @Select("""
            SELECT q.type  AS type,
                   COUNT(*) AS total,
                   SUM(CASE WHEN r.is_correct = 1 THEN 1 ELSE 0 END) AS correct
            FROM practice_session_question psq
            JOIN question q ON q.id = psq.question_id
            LEFT JOIN practice_record r
                   ON r.session_id = psq.session_id AND r.question_id = psq.question_id
            WHERE psq.session_id = #{sessionId}
            GROUP BY q.type
            ORDER BY q.type
            """)
    List<TypeStatVO> countByType(@Param("sessionId") Long sessionId);
}
