package com.asteria.pojo.entity.VO;

import lombok.Data;

/** 错题按题型分布的一项（对应接口文档 byType[] 的元素） */
@Data
public class WrongTypeCountVO {

    /** 题型：SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK */
    private String type;

    /** 该题型的错题数（前端标签绑的是 count，SQL 别名必须一致） */
    private Integer count;
}
