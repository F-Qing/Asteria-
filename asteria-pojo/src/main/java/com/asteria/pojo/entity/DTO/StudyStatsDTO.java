package com.asteria.pojo.entity.DTO;

import lombok.Data;

/**
 * 学习统计的原始聚合值（数据库查出来的，不是直接返回给前端的）。
 *
 * <p>单独一个 DTO 而不是让 SQL 直接把正确率算好，是为了让"正确率怎么算"
 * 只存在于 Java 的那一个方法里（calcAccuracy）—— 会话结果接口也用它。
 * 两处各写一份公式，以后改口径必然漏一个。
 */
@Data
public class StudyStatsDTO {

    /** 累计作答次数（SUM(answered_count)） */
    private Integer practiced;

    /** 累计答对数（SUM(correct_count)） */
    private Integer correct;
}
