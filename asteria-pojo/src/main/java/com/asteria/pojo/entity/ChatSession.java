package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话实体，对应 finaltext.chat_session 表。
 *
 * <p>createdAt / updatedAt 标了 fill：项目的 MybatisPlusConfig 里有 MetaObjectHandler，
 * 插入时会自动把这两个字段填上。不标的话 Java 对象里是 null（只能靠数据库默认值，
 * 结果就是 insert 完拿不到创建时间，还得再查一次库）。
 */
@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题（首条用户消息前 20 字自动生成，用户可改） */
    private String title;

    /** 模式：BUILTIN 内置模型 / EXTERNAL 外部 */
    private String agentMode;

    /** 消息条数（冗余字段：列表页显示"共 N 条"用，每插一条消息 +1） */
    private Integer messageCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
