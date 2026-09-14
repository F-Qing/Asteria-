package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.util.List;

/** 错题统计 VO，对应接口文档「查询错题统计」的 data（题库详情页右侧那个面板） */
@Data
public class WrongStatsVO {

    private Long bankId;

    /** 待攻克错题总数（= byType 各题型之和） */
    private Integer wrongTotal;

    /** 按题型分布 */
    private List<WrongTypeCountVO> byType;

    /** 按章节分布 */
    private List<WrongChapterCountVO> byChapter;
}
