package com.asteria.server.controller;

import com.asteria.common.result.ApiResponse;
import com.asteria.pojo.entity.VO.BankDetailVO;
import com.asteria.pojo.entity.VO.BankResultVO;
import com.asteria.pojo.entity.VO.BanksVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import com.asteria.server.Services.BanksService;
import com.asteria.server.ai.AiHeaders;
import com.asteria.server.ai.AiRequestConfig;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * multipart 接收：file 必传，bankName 可选（缺省由前端取文件名），
     * aiParse 可选（true = 入库后用 AI 逐题生成解析，缺答案的题顺带补答案）
     */
    @PostMapping("/import")
    public ApiResponse<BanksVO> importBanks(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bankName", required = false) String bankName,
            @RequestParam(value = "aiParse", defaultValue = "false") boolean aiParse,
            HttpServletRequest request) throws IOException {

        // AI 配置从请求头取好再传给 Service（Service 不碰 HttpServletRequest，这是分层约定）。
        // 勾了 AI 解析却没配置 AI → 直接抛 40020，别让用户以为解析过了。
        AiRequestConfig aiConfig = aiParse ? AiHeaders.require(request) : null;

        log.info("收到上传：fileName={}, size={}B, bankName={}, aiParse={}",
                file.getOriginalFilename(), file.getSize(), bankName, aiParse);
        return ApiResponse.ok(banksService.importBanks(file, bankName, aiConfig));
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
