package com.asteria.pojo.entity.VO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 题库详情 = 题库列表项的全部字段（继承自 BanksVO）+ 错题数 + 章节列表。
 *
 * <p>继承是为了复用列表那 12 个字段，不用抄一遍；列表接口只返回 BanksVO，
 * 不会带上这里的 chapters / wrongCount。
 */
@Data
@EqualsAndHashCode(callSuper = true)     // 继承 + Lombok @Data 必须加，否则 equals 会漏掉父类字段
public class BankDetailVO extends BanksVO {

    /** 错题总数（错题功能做完后才有真值） */
    private Integer wrongCount;

    /** 章节列表（每章带题目数） */
    private List<ChapterVO> chapters;
}
