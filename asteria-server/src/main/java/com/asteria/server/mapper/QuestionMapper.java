package com.asteria.server.mapper;

import com.asteria.pojo.entity.DTO.BankCountDTO;
import com.asteria.pojo.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** 题目 Mapper（表 question） */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 按「题库 + 题型」统计题目数，一次查完传进来的一批题库（避免每个题库查一次）。
     *
     * <p>返回的每一行形如 (bankId=12, type="SINGLE", cnt=40)。
     */
    @Select("""
            <script>
            SELECT bank_id, type, COUNT(*) AS cnt
            FROM question
            WHERE bank_id IN
            <foreach collection="bankIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY bank_id, type
            </script>
            """)
    List<BankCountDTO> countGroupByBankAndType(@Param("bankIds") Collection<Long> bankIds);
}
