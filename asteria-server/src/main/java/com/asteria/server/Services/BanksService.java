package com.asteria.server.Services;

import com.asteria.pojo.entity.VO.BankDetailVO;
import com.asteria.pojo.entity.VO.BankResultVO;
import com.asteria.pojo.entity.VO.BanksVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BanksService {

    /** M1 上传 MVP：校验 → 存盘 → 登记任务 → 立刻返回 taskId */
    BanksVO importBanks(MultipartFile file, String bankName) throws IOException;

    /** 查询导入任务进度（内存优先，数据库兜底） */
    BankResultVO Message(String taskId);

    /** 题库分页查询：keyword 模糊匹配题库名/文件名；page 从 1 开始 */
    PageResultVO<BanksVO> pageQuery(String keyword, Integer page, Integer pageSize);

    /** 题库详情（含章节列表）；题库不存在返回 null，由 Controller 翻译成 40401 */
    BankDetailVO getBankDetail(Long id);

    void deleteBank(Long id);
}
