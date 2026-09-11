package com.asteria.server.Services.impl;

import com.asteria.pojo.entity.DTO.TextDateTimeDTO;
import com.asteria.pojo.entity.TextDateTime;
import com.asteria.pojo.entity.VO.TextDateTimeVO;
import com.asteria.pojo.enums.ExamLevel;
import com.asteria.server.Services.TextDateService;
import com.asteria.server.mapper.TextDateTimeMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TextDateServiceImpl implements TextDateService {

    private final TextDateTimeMapper textDateTimeMapper;

    @Override
    public void addText(TextDateTimeDTO dto) {
        TextDateTime entity = new TextDateTime();
        entity.setName(dto.getName());
        entity.setDate(parseDate(dto.getDate()));
        textDateTimeMapper.insert(entity);
        log.info("新增考试成功：name={}, date={}", entity.getName(), entity.getDate());
    }

    @Override
    public List<TextDateTimeVO> listAll() {
        List<TextDateTime> list = textDateTimeMapper.selectList(
                Wrappers.<TextDateTime>lambdaQuery().orderByAsc(TextDateTime::getDate));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public boolean updateText(Long id, TextDateTimeDTO dto) {
        TextDateTime entity = textDateTimeMapper.selectById(id);
        if (entity == null) {
            log.warn("修改失败：考试不存在 id={}", id);
            return false;
        }
        entity.setName(dto.getName());
        entity.setDate(parseDate(dto.getDate()));
        textDateTimeMapper.updateById(entity);
        log.info("修改考试成功：id={}, name={}, date={}", id, entity.getName(), entity.getDate());
        return true;
    }

    @Override
    public boolean deleteText(Long id) {
        // deleteById 返回受影响行数：0 = 不存在
        int rows = textDateTimeMapper.deleteById(id);
        if (rows == 0) {
            log.warn("删除失败：考试不存在 id={}", id);
            return false;
        }
        log.info("删除考试成功：id={}", id);
        return true;
    }

    /** 实体 → VO */
    private TextDateTimeVO toVO(TextDateTime entity) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), entity.getDate());
        TextDateTimeVO vo = new TextDateTimeVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDate(entity.getDate().toString());
        vo.setDaysLeft(daysLeft);
        vo.setLevel(ExamLevel.of(daysLeft).getValue());
        return vo;
    }

    /** "YYYY-MM-DD" 字符串 → LocalDate（新增/修改共用） */
    private LocalDate parseDate(String s) {
        return LocalDate.parse(s.trim());
    }
}