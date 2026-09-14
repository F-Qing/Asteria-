package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷题会话实体，对应 finaltext.practice_session 表。
 *
 * <p>一次「开始刷题」= 1 条记录。会话里到底考哪几道题、什么顺序，
 * 存在 practice_session_question 表里（随机模式也必须固定顺序，否则重进会话题目就乱了）。
 */
@Data
@TableName("practice_session")
public class PracticeSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属题库 id（外键 → bank.id，删题库会级联删掉会话） */
    private Long bankId;

    /** 出题顺序：SEQUENTIAL 顺序 / RANDOM 随机 */
    private String mode;

    /** 题型：SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK/ALL */
    private String questionType;

    /** 章节筛选；null = 全章节 */
    private Long chapterId;

    /** 状态：IN_PROGRESS 进行中 / COMPLETED 已完成 */
    private String status;

    /** 题目总数（建会话时定下，之后不变） */
    private Integer totalCount;

    /** 已答题数（提交答案时 +1） */
    private Integer answeredCount;

    /** 答对数（提交答案且判对时 +1） */
    private Integer correctCount;

    /** 来源：NORMAL 普通刷题 / WRONG 错题重刷 */
    private String sessionSource;

    /** 完成时间（最后一题提交后写入） */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
