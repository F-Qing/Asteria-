package com.asteria.pojo.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 分页返回结构：{ list, total, page, pageSize }（对应前端 PageData&lt;T&gt;） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页记录数 */
    private long pageSize;
}
