package com.asteria.server.Services;

import com.asteria.pojo.entity.VO.BankDetailVO;
import com.asteria.pojo.entity.VO.BankResultVO;
import com.asteria.pojo.entity.VO.BanksVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import com.asteria.server.ai.AiRequestConfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BanksService {

    /**
     * 上传：校验 → 存盘 → 登记任务 → 立刻返回 taskId（解析与入库在后台线程里做）。
     *
     * @param aiConfig AI 配置；传 null = 不做 AI 解析，非 null = 入库后逐题生成解析
     *                 （配置从请求头来，由 Controller 用 AiHeaders 取好再传进来）
     */
    BanksVO importBanks(MultipartFile file, String bankName, AiRequestConfig aiConfig) throws IOException;

    /** 查询导入任务进度（内存优先，数据库兜底） */
    BankResultVO Message(String taskId);

    /** 题库分页查询：keyword 模糊匹配题库名/文件名；page 从 1 开始 */
    PageResultVO<BanksVO> pageQuery(String keyword, Integer page, Integer pageSize);

    /** 题库详情（含章节列表）；题库不存在返回 null，由 Controller 翻译成 40401 */
    BankDetailVO getBankDetail(Long id);

    void deleteBank(Long id);
}
