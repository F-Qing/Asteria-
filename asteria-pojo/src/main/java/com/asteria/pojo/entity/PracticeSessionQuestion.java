package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会话题目清单实体，对应 finaltext.practice_session_question 表。
 *
 * <p>这张表解决两个问题：
 * 1) 这次会话到底有哪些题（total_count 只是个数字，具体是哪些题得存下来）；
 * 2) 出题顺序（sort）—— 随机模式不存顺序的话，用户中途退出再进来题目就换了，
 *    已答记录还会跟题号对不上。
 */
@Data
@TableName("practice_session_question")
public class PracticeSessionQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 id（外键 → practice_session.id） */
    private Long sessionId;

    /** 题目 id（外键 → question.id） */
    private Long questionId;

    /** 第几题，从 1 开始（随机模式也固定下来） */
    private Integer sort;
}
