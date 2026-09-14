package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 错题本实体，对应 finaltext.wrong_question 表。
 *
 * <p>按 (bank_id, question_id) 唯一：一道题只留一条，错多次只累加 wrong_count。
 * resolved 由「错题重刷答对」置 1（已攻克，不再计入统计、也不再被重刷选中）。
 */
@Data
@TableName("wrong_question")
public class WrongQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属题库 id */
    private Long bankId;

    /** 题目 id */
    private Long questionId;

    /** 最近一次的错误作答（错题回顾/结果页直接用） */
    private String userAnswer;

    /** 累计答错次数 */
    private Integer wrongCount;

    /**
     * 是否已攻克：0 待攻克 / 1 已攻克
     *
     * <p>库里是 tinyint(1)，这里刻意用 Integer 而不是 Boolean 承接 ——
     * Boolean 要走 MyBatis 的类型转换，0/1 与 false/true 的映射多一层隐性依赖。
     */
    private Integer resolved;
}
