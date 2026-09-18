package com.asteria.common.Tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawQuestion {
    private String rawType;                 // 题型
    private String rawStem;                 // 题干
    private List<QuestionOption> rawOptions;// 选项列表
    private String rawAnswer;               // 正确答案
    /**
     * 所属章节名（题目前面最近的那个章节标题）。
     *
     * <p>null / 空 = 原文里没有章节信息（或者这几道题出现在任何章节标题之前），
     * 入库时统一归到「默认章节」。
     */
    private String chapterName;
    private int startLine;                  // 开始行号
    private List<String> suspicious;        // 可疑记录

}
