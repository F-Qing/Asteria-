package com.asteria.pojo.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 导入任务快照：GET /api/banks/import/{taskId} 的返回体（接口文档「查询导入任务进度」） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankResultVO {
    private String taskId;         // 任务 ID（UUID）
    private String status;         // ImportStatus.name()
    private String fileName;       // 用户上传的原始文件名
    private Long fileSize;         // 文件大小（字节）
    private Integer progress;      // 进度 0-100（文档定义为整数）
    private Long bankId;           // 成功后才有值，其余阶段为 null
    private Integer totalCount;    // 识别题目总数
    private String errorMessage;   // FAILED 时的失败原因
}