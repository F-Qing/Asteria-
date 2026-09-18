package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点总结实体，对应 finaltext.knowledge_summary 表。
 *
 * <p>5 个数组在库里是 JSON 列，这里用 String 承接原始 JSON 文本，
 * 由 Service 层用 Jackson 转 List&lt;String&gt; —— 和 Question.options 一个套路。
 *
 * <p>generatedAt 对外返回的是 updatedAt（DDL 里定的约定：重新生成时 updated_at 会刷新）。
 */
@Data
@TableName("knowledge_summary")
public class KnowledgeSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属题库 id（唯一键：一个题库只有一份） */
    private Long bankId;

    /** 重点摘要（JSON 字符串数组） */
    private String highlights;

    /** 核心知识点（JSON 字符串数组） */
    private String keyPoints;

    /** 高频考点（JSON 字符串数组） */
    private String hotTopics;

    /** 易错点（JSON 字符串数组） */
    private String easyMistakes;

    /** 学习建议（JSON 字符串数组） */
    private String studySuggestions;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
