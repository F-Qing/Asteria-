package com.asteria.server.mapper;

import com.asteria.pojo.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 聊天消息 Mapper（表 chat_message）；CRUD 全靠 BaseMapper，不用写 SQL */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
