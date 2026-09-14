package com.asteria.pojo.entity;

import com.asteria.pojo.entity.VO.QuestionVO;
import lombok.Data;

import java.util.List;

/**
 * 题目分页查询的返回结构（对应接口文档 { list, total, page, pageSize }）。
 *
 * <p>注意：list 里装的是 QuestionVO，不是表实体 Question ——
 * 因为接口要返回 chapterName（要连表查章节名）和 knowledgePoints（JSON 列要转成数组）。
 */
@Data
public class QuestionPage {

    /** 当前页题目列表 */
    private List<QuestionVO> list;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页记录数 */
    private long pageSize;
}
