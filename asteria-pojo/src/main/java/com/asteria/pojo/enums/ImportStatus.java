package com.asteria.pojo.enums;

/** 导入任务状态机：PENDING → PARSING →（解析不出题时 AI_FORMATTING）→ AI_PROCESSING → SUCCESS / FAILED */
public enum ImportStatus {
    PENDING,//待处理
    PARSING,//解析中
    AI_FORMATTING,//AI 整理格式中（文本解析不出题目时的兜底）
    AI_PROCESSING,//AI处理中
    SUCCESS,//成功
    FAILED//失败
}
