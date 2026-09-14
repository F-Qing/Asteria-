package com.asteria.pojo.entity.VO;

import lombok.Data;

/**
 * 题目选项（给前端看的）。
 *
 * <p>为什么 pojo 里要单独来一个：解析阶段用的 {@code com.asteria.common.Tool.QuestionOption}
 * 在 common 模块，而 pojo **看不到 common**（依赖方向是 server → common/pojo）。
 * 所以展示层用这个 pojo 版，Service 把数据库里的 options JSON 反序列化成它。
 */
@Data
public class QuestionOptionVO {

    /** 选项标识：A / B / C / D */
    private String key;

    /** 选项内容 */
    private String text;
}
