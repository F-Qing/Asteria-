package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 题库实体，对应 finaltext.bank 表（一次成功导入产出 1 个题库） */
@Data
@TableName("bank")
public class Bank {

    /** 自增主键，对外就是接口里的题库 id */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题库名称：上传时填的 bankName，缺省取文件名 */
    private String name;

    /** 来源文件名 */
    private String fileName;

    /** 来源文件类型：docx/pdf/txt */
    private String fileType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
