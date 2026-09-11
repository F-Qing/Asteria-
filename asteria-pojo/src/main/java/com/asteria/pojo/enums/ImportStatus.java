package com.asteria.pojo.enums;

/** 导入任务状态机：PENDING → PARSING → AI_PROCESSING → SUCCESS / FAILED（后两个是终态） */
public enum ImportStatus {
    PENDING,//待处理
    PARSING,//解析中
    AI_PROCESSING,//AI处理中
    SUCCESS,//成功
    FAILED//失败
}
