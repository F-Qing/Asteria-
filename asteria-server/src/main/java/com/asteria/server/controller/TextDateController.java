package com.asteria.server.controller;

import com.asteria.common.result.ApiResponse;
import com.asteria.pojo.entity.DTO.TextDateTimeDTO;
import com.asteria.pojo.entity.VO.TextDateTimeVO;
import com.asteria.server.Services.TextDateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/config")
public class TextDateController {

    @Autowired
    private TextDateService textDateService;

    @PostMapping("/exams")
    public ApiResponse addText(@RequestBody TextDateTimeDTO dto) {
        log.info("新增考试日期：{}", dto);
        textDateService.addText(dto);
        return ApiResponse.ok();
    }

    /** 查询考试倒计时列表：id/name/date 来自数据库，daysLeft/level 现算返回 */
    @GetMapping("/exams")
    public ApiResponse<List<TextDateTimeVO>> list() {
        return ApiResponse.ok(textDateService.listAll());
    }

    /** 修改考试：按 id 更新 name/date；记录不存在返回 code=40401 */
    @PutMapping("/exams/{id}")
    public ApiResponse<Void> updateText(@PathVariable Long id, @RequestBody TextDateTimeDTO dto) {
        log.info("修改考试：id={}, dto={}", id, dto);
        boolean ok = textDateService.updateText(id, dto);
        return ok ? ApiResponse.ok() : ApiResponse.error(40401, "考试记录不存在：" + id);
    }

    /** 删除考试：按 id 删除；记录不存在返回 code=40401 */
    @DeleteMapping("/exams/{id}")
    public ApiResponse<Void> deleteText(@PathVariable Long id) {
        log.info("删除考试：id={}", id);
        boolean ok = textDateService.deleteText(id);
        return ok ? ApiResponse.ok() : ApiResponse.error(40401, "考试记录不存在：" + id);
    }
}