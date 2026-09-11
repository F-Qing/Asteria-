package com.asteria.pojo.entity.DTO;

import lombok.Data;

/**
 * 聚合计数的通用结果（不是某张表的实体，而是"统计查询出来的一行"）。
 *
 * <p>题目统计：bankId + type + cnt 都有值，例如 (12, "SINGLE", 40)；
 * 章节统计：只有 bankId + cnt，type 为 null。
 */
@Data
public class BankCountDTO {

    /** 题库 id（对应 SQL 里的 bank_id） */
    private Long bankId;

    /** 题型（对应 SQL 里的 type），章节统计时为 null */
    private String type;

    /** 数量（对应 SQL 里的 COUNT(*) AS cnt） */
    private Integer cnt;
}
