package com.asteria.server.controller;

import com.asteria.common.result.ApiResponse;
import com.asteria.pojo.entity.VO.BankDetailVO;
import com.asteria.pojo.entity.VO.BankResultVO;
import com.asteria.pojo.entity.VO.BanksVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import com.asteria.server.Services.BanksService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/api/banks")
public class BanksController {

    @Autowired
    private BanksService banksService;

    /** 题库分页查询：GET /api/banks?keyword=&page=1&pageSize=100 */
    @GetMapping
    public ApiResponse<PageResultVO<BanksVO>> pageQuery(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize) {
        return ApiResponse.ok(banksService.pageQuery(keyword, page, pageSize));
    }

    /** 查询题库详情：GET /api/banks/{id}（含章节列表） */
    @GetMapping("/{id}")
    public ApiResponse<BankDetailVO> getBankDetail(@PathVariable Long id) {
        BankDetailVO detail = banksService.getBankDetail(id);
        if (detail == null) {
            return ApiResponse.error(40401, "题库不存在：" + id);
        }
        return ApiResponse.ok(detail);
    }

    /** multipart 接收：file 必传，bankName 可选（缺省由前端取文件名） */
    @PostMapping("/import")
    public ApiResponse<BanksVO> importBanks(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bankName", required = false) String bankName) throws IOException {
        log.info("收到上传：fileName={}, size={}B, bankName={}",
                file.getOriginalFilename(), file.getSize(), bankName);
        return ApiResponse.ok(banksService.importBanks(file, bankName));
    }

    /** 查询导入任务进度：GET /api/banks/import/{taskId} */
    @GetMapping("/import/{taskId}")
    public ApiResponse<BankResultVO> getImportTask(@PathVariable String taskId) {
        return ApiResponse.ok(banksService.Message(taskId));
    }

    /** 删除题库：DELETE /api/banks/{id}（章节、题目靠外键级联删除） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBank(@PathVariable Long id) {
        banksService.deleteBank(id);
        return ApiResponse.ok();
    }
}
