package com.asteria.server.mapper;

import com.asteria.pojo.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 聊天会话 Mapper（表 chat_session）；CRUD 全靠 BaseMapper，不用写 SQL */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
