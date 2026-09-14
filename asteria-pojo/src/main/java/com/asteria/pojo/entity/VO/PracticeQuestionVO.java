package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.util.List;

/**
 * 刷题中的题目 VO —— 和 QuestionVO 的唯一区别：<b>没有 answer / analysis</b>。
 *
 * <p>这就是"防偷看"：刷题时如果把标准答案一起返回给前端，
 * 打开 F12 的 Network 面板就能看到答案。答案只在该题提交后，
 * 由「提交单题答案」接口单独返回。
 *
 * <p>所以这里刻意不复用 QuestionVO（那个带答案），宁可多一个类。
 */
@Data
public class PracticeQuestionVO {

    private Long id;

    private Long bankId;

    private Long chapterId;

    /** 章节名（chapter 表查出来的，不在 question 里） */
    private String chapterName;

    private String type;

    private String stem;

    /** 选项（判断题/填空题/简答题为 null） */
    private List<QuestionOptionVO> options;

    /** 知识点标签 */
    private List<String> knowledgePoints;
}
