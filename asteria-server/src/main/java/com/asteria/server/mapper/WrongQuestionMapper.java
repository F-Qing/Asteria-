package com.asteria.server.mapper;

import com.asteria.pojo.entity.VO.WrongChapterCountVO;
import com.asteria.pojo.entity.VO.WrongTypeCountVO;
import com.asteria.pojo.entity.WrongQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 错题本 Mapper（表 wrong_question）。
 *
 * <p>收录错题、标记已攻克这些增改都用 MyBatis-Plus 现成的；
 * 下面两个手写 SQL 是"按题型 / 按章节"的分布统计。
 */
@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /**
     * 错题按题型统计（只算"待攻克"的）。
     *
     * <p>只查 wrong_question 一个数字列，题型从 question 表 JOIN 取 ——
     * 错题本自己没存题型，避免两处各存一份、以后改题库时对不上。
     *
     * <p>别名 count 用了反引号：COUNT 是 MySQL 的函数名，虽然当别名一般也能用，
     * 加反引号彻底免掉关键字风险（项目里 question.options 列也是这么处理的）。
     * 别名必须叫 count —— 前端标签绑的是 t.count。
     */
    @Select("""
            SELECT q.type AS type, COUNT(*) AS `count`
            FROM wrong_question wq
            JOIN question q ON q.id = wq.question_id
            WHERE wq.bank_id = #{bankId} AND wq.resolved = 0
            GROUP BY q.type
            ORDER BY COUNT(*) DESC, q.type
            """)
    List<WrongTypeCountVO> countGroupByType(@Param("bankId") Long bankId);

    /**
     * 错题按章节统计（只算"待攻克"的）。
     *
     * <p>要跳两次表：错题本只有 question_id → question 有 chapter_id → chapter 才有章节名。
     *
     * <p>按 chapter.sort 排，不是按错题数排 —— 章节本身有先后顺序，
     * 显示成"第一章 3 道、第二章 1 道"比"错得多的排前面"更符合翻书直觉。
     *
     * <p>c.id / c.name 必须起驼峰别名：靠下划线转驼峰只能处理 chapter_id 这种列名，
     * c.id 转不出 chapterId，不起别名 VO 里就是 null。
     */
    @Select("""
            SELECT c.id AS chapterId, c.name AS chapterName, COUNT(*) AS `count`
            FROM wrong_question wq
            JOIN question q ON q.id = wq.question_id
            JOIN chapter c ON c.id = q.chapter_id
            WHERE wq.bank_id = #{bankId} AND wq.resolved = 0
            GROUP BY c.id, c.name, c.sort
            ORDER BY c.sort
            """)
    List<WrongChapterCountVO> countGroupByChapter(@Param("bankId") Long bankId);
}
