package com.asteria.pojo.entity.VO;

import lombok.Data;

/** 章节（题库详情里的章节列表项） */
@Data
public class ChapterVO {

    private Long id;

    /** 所属题库 id */
    private Long bankId;

    /** 章节名 */
    private String name;

    /** 排序 */
    private Integer sort;

    /** 这一章有多少题（JOIN 统计出来的，不是 chapter 表的列） */
    private Integer questionCount;
}
