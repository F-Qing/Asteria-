package com.asteria.server.mapper;

import com.asteria.pojo.entity.PracticeRecord;
import com.asteria.pojo.entity.VO.WrongQuestionItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 答题记录 Mapper（表 practice_record） */
@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {

    /**
     * 查本次会话的错题明细（题干 + 标准答案），按作答先后排。
     *
     * <p>只取 is_correct = 0 的：简答题的 is_correct 是 NULL（没判分），不算错题。
     *
     * <p>列名 question_id / user_answer 会自动映射到 VO 的 questionId / userAnswer
     * （application.yml 里开了 map-underscore-to-camel-case）。
     */
    @Select("""
            SELECT r.question_id, q.stem, q.type, r.user_answer, q.answer
            FROM practice_record r
            JOIN question q ON q.id = r.question_id
            WHERE r.session_id = #{sessionId} AND r.is_correct = 0
            ORDER BY r.id
            """)
    List<WrongQuestionItemVO> selectWrongItems(@Param("sessionId") Long sessionId);
}
