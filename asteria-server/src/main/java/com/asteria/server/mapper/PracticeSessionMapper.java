package com.asteria.server.mapper;

import com.asteria.pojo.entity.DTO.StudyStatsDTO;
import com.asteria.pojo.entity.PracticeSession;
import com.asteria.pojo.entity.VO.RecentSessionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 刷题会话 Mapper（表 practice_session） */
@Mapper
public interface PracticeSessionMapper extends BaseMapper<PracticeSession> {

    /**
     * 按 practice_record 重算会话的已答 / 答对数量。
     *
     * <p>为什么"重算"而不是在 Java 里 +1 / -1：
     * 重答一道题时答对数可能要减（原来对、这次错），加减分支很容易算漂。
     * 直接以答题记录为准重算，不管第几次作答，结果永远和 practice_record 一致。
     *
     * <p>子查询查的是 practice_record、更新的是 practice_session，是两张不同的表，
     * 不会触发 MySQL 的 "You can't specify target table for update in FROM clause"（1093）。
     */
    @Update("""
            UPDATE practice_session ps
            SET ps.answered_count = (SELECT COUNT(*) FROM practice_record WHERE session_id = ps.id),
                ps.correct_count  = (SELECT COUNT(*) FROM practice_record WHERE session_id = ps.id AND is_correct = 1)
            WHERE ps.id = #{sessionId}
            """)
    int refreshCounts(@Param("sessionId") Long sessionId);

    /**
     * 学习统计：累计作答次数 + 累计答对数（首页统计卡用）。
     *
     * <p>口径和「查询会话结果」接口保持一致：
     * practiced = SUM(answered_count)，正确率的分母也是它 —— 两处算出来的数不会打架。
     *
     * <p>一条会话都没有时 SUM 返回 NULL，用 IFNULL 兜成 0，省得 Java 里判空。
     * <p>bankId 可选：用 &lt;if&gt; 动态拼 WHERE，不传就是统计全部题库。
     */
    @Select("""
            <script>
            SELECT IFNULL(SUM(answered_count), 0) AS practiced,
                   IFNULL(SUM(correct_count), 0)  AS correct
            FROM practice_session
            <where>
                <if test="bankId != null">bank_id = #{bankId}</if>
            </where>
            </script>
            """)
    StudyStatsDTO selectStudyStats(@Param("bankId") Long bankId);

    /**
     * 最近刷题列表（首页「最近刷题」卡片用）。
     *
     * <p>按 id 倒序，不用 created_at 倒序：id 自增，本身就是创建顺序，而且永远唯一 ——
     * created_at 同一秒创建的两个会话顺序会不确定。
     *
     * <p>LIMIT 用 #{} 占位符是安全的：MySQL 的预处理语句支持 LIMIT ?，
     * 而且 limit 在 Service 里已经做过兜底和上限，不会把用户输入直接拼进 SQL。
     */
    @Select("""
            SELECT ps.id, ps.bank_id, b.name AS bankName, ps.mode, ps.status,
                   ps.total_count, ps.answered_count, ps.correct_count, ps.created_at
            FROM practice_session ps
            JOIN bank b ON b.id = ps.bank_id
            ORDER BY ps.id DESC
            LIMIT #{limit}
            """)
    List<RecentSessionVO> selectRecent(@Param("limit") int limit);
}
