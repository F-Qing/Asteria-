package com.asteria.server.mapper;

import com.asteria.pojo.entity.BankImport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 导入任务 Mapper（表 import_task） */
@Mapper
public interface BanksImportMapper extends BaseMapper<BankImport> {
}
