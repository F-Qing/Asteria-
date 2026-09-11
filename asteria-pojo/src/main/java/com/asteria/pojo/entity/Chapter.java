package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 章节实体，对应 finaltext.chapter 表；文件里没有章节信息时用“默认章节”兜底 */
@Data
@TableName("chapter")
public class Chapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属题库 id（外键 → bank.id，删题库级联删章节） */
    private Long bankId;

    /** 章节名 */
    private String name;

    /** 排序，前端按此升序展示 */
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
