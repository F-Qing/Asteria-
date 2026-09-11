package com.asteria.server.Services;

import com.asteria.pojo.entity.DTO.TextDateTimeDTO;
import com.asteria.pojo.entity.VO.TextDateTimeVO;

import java.util.List;

public interface TextDateService {
    void addText(TextDateTimeDTO dto);
    List<TextDateTimeVO> listAll();
    /** 修改考试；返回 false 表示该 id 不存在 */
    boolean updateText(Long id, TextDateTimeDTO dto);
    /** 删除考试；返回 false 表示该 id 不存在 */
    boolean deleteText(Long id);
}