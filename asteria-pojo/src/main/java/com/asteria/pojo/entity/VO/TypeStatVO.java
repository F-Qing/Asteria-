package com.asteria.pojo.entity.VO;

import lombok.Data;

/** 按题型统计的一项（对应接口文档 typeStats[] 的元素） */
@Data
public class TypeStatVO {

    /** 题型：SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK */
    private String type;

    /** 本次会话里这个题型一共几题（含没答的） */
    private Integer total;

    /** 这个题型答对几题 */
    private Integer correct;
}
