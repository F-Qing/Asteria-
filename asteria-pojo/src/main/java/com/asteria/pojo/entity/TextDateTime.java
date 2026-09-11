package com.asteria.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 考试倒计时实体，对应 finaltext.textdatetime 表 */
@Data
@TableName("textdatetime")
public class TextDateTime {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalDate date;
}