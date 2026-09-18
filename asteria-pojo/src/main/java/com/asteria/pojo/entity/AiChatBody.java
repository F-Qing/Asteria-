package com.asteria.pojo.entity;

import lombok.Data;

@Data
public class AiChatBody {
    private String content;
    private String agentMode="BUILTIN";
}
