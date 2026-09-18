package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识点总结 VO —— 接口文档「知识点总结接口」里两个接口的 data 结构：
 * 「生成知识点总结」（POST）必有，「查询已生成总结」（GET）没有总结时为 null。
 *
 * <p>字段和顺序严格照文档，改之前先看 generate-api-doc.ps1 里对应的那段；
 * 改了要同步重跑脚本生成接口文档。
 *
 * <p>五个数组统一是 {@code List<String>}，Jackson 直接序列化成 JSON 数组；
 * 库里存的是 JSON 字符串，由 Service 层转。
 *
 * <p>为什么放 pojo：和别的 VO 一样，Controller / Service 两边都能直接用，
 * pojo 不依赖 server。
 */
@Data
public class AiSummaryVo {

    /** 总结 ID（知识总结表的主键） */
    private Long id;

    /** 题库 ID */
    private Long bankId;

    /** 题库名称（列表页/详情页要显示，避免前端再查一次题库） */
    private String bankName;

    /** 重点摘要 */
    private List<String> highlights;

    /** 核心知识点 */
    private List<String> keyPoints;

    /** 高频考点 */
    private List<String> hotTopics;

    /** 易错点 */
    private List<String> easyMistakes;

    /** 学习建议 */
    private List<String> studySuggestions;

    /** 生成时间；序列化后是 ISO 字符串，对应文档的 string */
    private LocalDateTime generatedAt;

    /** 是否命中缓存：true = 直接取的上一次结果，没有真的调 AI */
    private Boolean fromCache;
}
