package com.asteria.pojo.entity.VO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 会话详情 VO = 会话本身的字段 + 题目列表 + 已答记录。
 *
 * <p>继承 PracticeSessionVO 复用那 9 个字段（和 BankDetailVO 继承 BanksVO 一个套路）；
 * 有继承就必须写 @EqualsAndHashCode(callSuper = true)，否则 Lombok 会警告
 * "生成 equals 时不会比较父类字段"。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PracticeSessionDetailVO extends PracticeSessionVO {

    /** 题目列表（按出题顺序 sort；不含 answer/analysis） */
    private List<PracticeQuestionVO> questions;

    /** 已答记录（没答的题不在里面） */
    private List<AnswerRecordVO> records;
}
