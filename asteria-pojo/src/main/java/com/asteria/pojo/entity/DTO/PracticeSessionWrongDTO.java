package com.asteria.pojo.entity.DTO;

import lombok.Data;

/**
 * 创建错题重刷会话的请求体，对应接口文档「创建错题重刷会话」的 Body。
 *
 * <p>刻意不复用 PracticeSessionCreateDTO：那个带 questionType / chapterId，
 * 而错题重刷是"题库里所有待攻克的错题"，题型和章节都不该由前端指定。
 * 用独立 DTO，前端多传的字段会被忽略，不会产生"传了但没生效"的误会。
 */
@Data
public class PracticeSessionWrongDTO {

    /** 题库 id（必传） */
    private Long bankId;

    /** 出题顺序：SEQUENTIAL/RANDOM；不传默认 RANDOM（重刷错题本来就该打乱） */
    private String mode;
}
