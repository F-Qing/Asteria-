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
    private int startLine;                  // 开始行号
    private List<String> suspicious;        // 可疑记录

}
