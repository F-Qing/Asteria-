package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答题记录实体，对应 finaltext.practice_record 表。
 *
 * <p>一道题在一次会话里的作答，按 (session_id, question_id) 唯一 —— 重答就覆盖那一行。
 */
@Data
@TableName("practice_record")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 id */
    private Long sessionId;

    /** 题目 id */
    private Long questionId;

    /** 用户作答：单选A / 多选ACD / 判断A=正确B=错误 / 填空多空用；分隔 / 简答文本 */
    private String userAnswer;

    /**
     * 是否正确：true 对 / false 错 / null 未判分（简答题，现在没有 AI 判分）
     *
     * <p>库里是 tinyint(1) 且允许 NULL，所以这里用 Boolean。
     * MyBatis 的 BooleanTypeHandler 会通过 ResultSet.wasNull() 区分
     * "false（答错）"和"null（没判分）"—— 这两个状态绝不能混。
     */
    private Boolean isCorrect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
