package com.asteria.server.mapper;

import com.asteria.pojo.entity.Chapter;
import com.asteria.pojo.entity.DTO.BankCountDTO;
import com.asteria.pojo.entity.VO.ChapterVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** 章节 Mapper（表 chapter） */
@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {

    /** 按题库统计章节数，每行形如 (bankId=12, type=null, cnt=1) */
    @Select("""
            <script>
            SELECT bank_id, COUNT(*) AS cnt
            FROM chapter
            WHERE bank_id IN
            <foreach collection="bankIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY bank_id
            </script>
            """)
    List<BankCountDTO> countGroupByBank(@Param("bankIds") Collection<Long> bankIds);

    /**
     * 查某个题库的章节列表，并顺带统计每章的题目数（一条 SQL 搞定，不用再查一次计数）。
     *
     * <p>要点：
     * <ul>
     *   <li>{@code LEFT JOIN}：没有题目的章节也会出现在结果里；</li>
     *   <li>{@code COUNT(q.id)} 不能写成 {@code COUNT(*)}：LEFT JOIN 没匹配到时 q.* 全是 NULL，
     *       COUNT(*) 会把那一行也数进去（变成 1），COUNT(q.id) 才是真正的 0；</li>
     *   <li>列名 bank_id / question_count 由 map-underscore-to-camel-case 自动映射到 ChapterVO。</li>
     * </ul>
     */
    @Select("""
            SELECT c.id, c.bank_id, c.name, c.sort, COUNT(q.id) AS question_count
            FROM chapter c
            LEFT JOIN question q ON q.chapter_id = c.id
            WHERE c.bank_id = #{bankId}
            GROUP BY c.id, c.bank_id, c.name, c.sort
            ORDER BY c.sort
            """)
    List<ChapterVO> selectChaptersWithCount(@Param("bankId") Long bankId);
}
