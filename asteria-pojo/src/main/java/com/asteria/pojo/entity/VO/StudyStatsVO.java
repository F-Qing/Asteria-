package com.asteria.pojo.entity.VO;

import lombok.Data;

/** 学习统计 VO，对应接口文档「查询学习统计」的 data（首页两张统计卡） */
@Data
public class StudyStatsVO {

    /** 已刷题：累计作答次数（同一题重做也计数） */
    private Integer practiced;

    /** 总体正确率：0~100 的整数（= 累计答对 / 累计作答；没答过为 0） */
    private Integer accuracy;
}
