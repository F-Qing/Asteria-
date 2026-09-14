package com.asteria.pojo.entity.VO;

import lombok.Data;

/** 错题按章节分布的一项（对应接口文档 byChapter[] 的元素） */
@Data
public class WrongChapterCountVO {

    private Long chapterId;

    /** 章节名（前端直接显示成标签） */
    private String chapterName;

    /** 该章节的错题数 */
    private Integer count;
}
