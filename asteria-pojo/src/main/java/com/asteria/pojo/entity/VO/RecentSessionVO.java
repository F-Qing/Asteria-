package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

/** 最近刷题列表的一项，对应接口文档「查询最近刷题列表」数组元素（首页「最近刷题」卡片） */
@Data
public class RecentSessionVO {

    private Long id;

    private Long bankId;

    /** 题库名（JOIN bank 拿的，前端直接显示） */
    private String bankName;

    /** SEQUENTIAL/RANDOM */
    private String mode;

    /** IN_PROGRESS/COMPLETED */
    private String status;

    private Integer totalCount;

    private Integer answeredCount;

    private Integer correctCount;

    /** 创建时间（Spring 会序列化成 ISO 字符串，前端 new Date(iso) 直接能用） */
    private LocalDateTime createdAt;
}
