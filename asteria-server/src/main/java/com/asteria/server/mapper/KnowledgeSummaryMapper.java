package com.asteria.server.mapper;

import com.asteria.pojo.entity.KnowledgeSummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 知识点总结 Mapper（表 knowledge_summary）；CRUD 全靠 BaseMapper，不用写 SQL */
@Mapper
public interface KnowledgeSummaryMapper extends BaseMapper<KnowledgeSummary> {
}
