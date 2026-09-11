package com.asteria.pojo.entity.VO;

import lombok.Data;

@Data
public class TextDateTimeVO {
    private Long id;
    private String name;
    private String date;
    private long daysLeft;
    private String level;
}