package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("import_task")
public class BankImport {
    @TableId
    private String taskId;
    private String bankName;
    private String fileName;
    private Long fileSize;
    private String status;
    private Integer progress;
    private Integer totalCount;
    private Long bankId;
    private String errorMessage;

    /** 创建时间：插入时由 MybatisPlusConfig 自动填充（列 created_at，DB 默认值兜底） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间：插入/更新时自动刷新（列 updated_at，DB ON UPDATE 兜底） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}