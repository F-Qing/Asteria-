package com.asteria.pojo.enums;

/** 导入任务状态机：PENDING → PARSING →（解析不出题时 AI_FORMATTING）→ AI_PROCESSING → SUCCESS / FAILED */
public enum ImportStatus {
    PENDING,//待处理
    PARSING,//解析中
    AI_FORMATTING,//AI 抽取中：规则解析不出题目时，让 AI 按 JSON 结构化抽取（枚举名保留，历史数据里已有这个值）
    AI_PROCESSING,//AI处理中
    SUCCESS,//成功
    FAILED//失败
}
