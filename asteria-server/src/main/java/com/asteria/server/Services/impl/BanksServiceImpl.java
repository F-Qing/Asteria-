package com.asteria.server.Services.impl;

import com.asteria.common.Tool.FileTextReader;
import com.asteria.common.Tool.QuestionParser;
import com.asteria.common.Tool.RawQuestion;
import com.asteria.common.exception.BusinessException;
import com.asteria.pojo.entity.Bank;
import com.asteria.pojo.entity.BankImport;
import com.asteria.pojo.entity.DTO.BankCountDTO;
import com.asteria.pojo.entity.VO.BankDetailVO;
import com.asteria.pojo.entity.VO.BankResultVO;
import com.asteria.pojo.entity.VO.BanksVO;
import com.asteria.pojo.entity.VO.ChapterVO;
import com.asteria.pojo.entity.VO.PageResultVO;
import com.asteria.pojo.enums.ImportStatus;
import com.asteria.server.Services.BanksImportTransactional;
import com.asteria.server.Services.BanksService;
import com.asteria.server.mapper.BankMapper;
import com.asteria.server.mapper.BanksImportMapper;
import com.asteria.server.mapper.ChapterMapper;
import com.asteria.server.mapper.QuestionMapper;
import com.asteria.server.tool.DocxTextReader;
import com.asteria.server.tool.PdfTextReader;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class BanksServiceImpl implements BanksService {

    /** 与接口文档约定一致：docx / pdf / txt、单文件 ≤ 20MB */
    private static final Set<String> ALLOWED_EXT = Set.of("docx", "pdf", "txt");
    private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;
    /** 存进 error_message 的摘要长度上限（列宽 500，这里留足余量） */
    private static final int MAX_REASON_LENGTH = 200;
    /** 分页查询单页上限，防止有人传 pageSize=100000 拉全表 */
    private static final int MAX_PAGE_SIZE = 200;

    /** 任务快照（taskId → 进度）。轮询接口优先读它，重启后回落到数据库 */
    private final ConcurrentHashMap<String, BankResultVO> importTasks = new ConcurrentHashMap<>();

    /** 上传目录可在配置里改，缺省为项目根下 uploads/ */
    @Value("${asteria.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private BanksImportMapper bankImportMapper;
    @Autowired
    private FileTextReader fileTextReader;
    @Autowired
    private DocxTextReader docxTextReader;
    @Autowired
    private PdfTextReader pdfTextReader;
    @Autowired
    private BanksImportTransactional importTransactional;
    @Autowired
    private BankMapper bankMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private ChapterMapper chapterMapper;

    /**
     * 上传入口：校验 → 存盘 → 登记任务 → 启动后台线程 → 立刻返回 taskId。
     *
     * <p>这里【不加】事务注解：只剩一条 insert，自动提交本身就是原子的；
     * 而且必须先提交，后台线程才更新得到这条任务记录。
     */
    @Override
    public BanksVO importBanks(MultipartFile file, String bankName) throws IOException {
        // 1) 校验：空文件 / 扩展名 / 大小
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40001, "请选择要上传的文件");
        }
        String originalName = file.getOriginalFilename();
        String ext = extOf(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(40002, "仅支持 docx/pdf/txt 文件，收到：" + originalName);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(40003, "文件超过 20MB 上限");
        }

        // 2) taskId：同时充当磁盘存储名（不使用原始文件名，防路径穿越/重名覆盖）
        String taskId = UUID.randomUUID().toString();

        // 3) 存盘
        Path destDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path destFile = destDir.resolve(taskId + "." + ext);
        if (bankName == null || bankName.isBlank()) {
            bankName = taskId;                      // 前端没传题库名时兜底
        }
        try {
            Files.createDirectories(destDir);
            file.transferTo(destFile.toFile());
        } catch (IOException e) {
            log.error("文件存盘失败：taskId={}, dest={}", taskId, destFile, e);
            throw new BusinessException(50001, "文件保存失败，请重试");
        }

        // 4) 登记任务（PENDING）
        try {
            bankImportMapper.insert(BankImport.builder()
                    .taskId(taskId)
                    .bankName(bankName)
                    .bankId(null)
                    .fileName(originalName)
                    .fileSize(file.getSize())
                    .status(ImportStatus.PENDING.name())
                    .progress(0)
                    .build());
        } catch (Exception e) {
            log.error("任务登记失败：taskId={}", taskId, e);
            deleteQuietly(destFile);
            throw new BusinessException(50002, "任务登记失败，请重试");
        }

        // 5) 内存快照（前端马上就会来轮询）
        importTasks.put(taskId, BankResultVO.builder()
                .taskId(taskId)
                .status(ImportStatus.PENDING.name())
                .fileName(originalName)
                .fileSize(file.getSize())
                .progress(0)
                .bankId(null)
                .build());

        log.info("导入任务已创建：taskId={}, fileName={}, bankName={}", taskId, originalName, bankName);

        // 6) 后台处理（解析 + 入库），主线程立刻返回
        String finalBankName = bankName;
        new Thread(() -> processImport(taskId, destFile, finalBankName, originalName, ext),
                "import-" + taskId).start();

        BanksVO vo = new BanksVO();
        vo.setTaskId(taskId);
        vo.setStatus(ImportStatus.PENDING.name());
        return vo;
    }

    // ========== 后台异步处理 ==========

    /**
     * 后台线程的三段式：解析（事务外）→ 入库（事务内）→ 更新内存（事务外）。
     * 失败时：内存标 FAILED + 独立事务写数据库 + 删掉磁盘文件。
     */
    private void processImport(String taskId, Path destFile, String bankName, String originalName, String ext) {
        try {
            // ① 解析：读文件 + 切块 + 提取（纯计算，不碰数据库，所以放在事务外面）
            // 按文件类型选"取文本"的方式：docx 用 POI、pdf 用 PDFBox、txt 直接按编码读
            // 三种方式抽出的都是"纯文本"，后面切块/判型/归一化的逻辑完全共用
            String content = switch (ext) {
                case "docx" -> docxTextReader.read(destFile);
                case "pdf" -> pdfTextReader.read(destFile);
                default -> fileTextReader.read(destFile);
            };
            List<RawQuestion> rawQuestions = new QuestionParser().parse(content);
            log.info("文件解析完成：taskId={}, 共{}条原始题", taskId, rawQuestions.size());

            importTasks.compute(taskId, (k, v) -> {
                if (v != null) {
                    v.setStatus(ImportStatus.PARSING.name());
                }
                return v;
            });
            // ② 入库：题库 + 章节 + 题目 + 任务状态，同一个事务（跨 Bean 调用，事务才生效）
            BanksImportTransactional.Outcome outcome =
                    importTransactional.saveImport(taskId, bankName, originalName, ext, rawQuestions);

            // ③ 内存状态：成功（内存不属于数据库事务，所以在事务外更新）
            importTasks.compute(taskId, (k, v) -> {
                if (v != null) {
                    v.setStatus(ImportStatus.SUCCESS.name());
                    v.setProgress(100);
                    v.setTotalCount(outcome.totalCount());
                    v.setBankId(outcome.bankId());
                }
                return v;
            });
            log.info("导入完成：taskId={}, bankId={}, 入库{}题, 跳过{}题",
                    taskId, outcome.bankId(), outcome.totalCount(), outcome.skippedCount());

        } catch (Exception e) {
            log.error("导入失败：taskId={}", taskId, e);
            String reason = briefReason(e);

            // 内存状态：失败
            importTasks.compute(taskId, (k, v) -> {
                if (v != null) {
                    v.setStatus(ImportStatus.FAILED.name());
                    v.setErrorMessage(reason);
                }
                return v;
            });

            // 数据库：用【独立事务】记失败（写在入库事务里会被回滚抹掉）
            try {
                importTransactional.markFailed(taskId, reason);
            } catch (Exception ex) {
                // 连"记失败"都失败了：只能记日志，绝不能再往外抛（否则线程直接死掉）
                log.error("写入失败状态时又出错：taskId={}", taskId, ex);
            }

            // 清理落盘文件（文件系统不受事务保护，得手动删）
            deleteQuietly(destFile);
        }
    }

    // ========== 查询接口 ==========

    @Override
    public BankResultVO Message(String taskId) {
        // 1) 先查内存（有实时进度，最新）
        BankResultVO task = importTasks.get(taskId);
        if (task != null) {
            return task;
        }

        // 2) 内存没有 → 查数据库（例如服务重启过）
        BankImport bankImport = bankImportMapper.selectById(taskId);
        if (bankImport != null) {
            return BankResultVO.builder()
                    .taskId(bankImport.getTaskId())
                    .status(bankImport.getStatus())
                    .fileName(bankImport.getFileName())
                    .fileSize(bankImport.getFileSize())
                    .progress(bankImport.getProgress())
                    .totalCount(bankImport.getTotalCount())
                    .bankId(bankImport.getBankId())
                    .errorMessage(bankImport.getErrorMessage())
                    .build();
        }

        // 3) 都没有 → 任务不存在
        return BankResultVO.builder()
                .taskId(taskId)
                .status(ImportStatus.FAILED.name())
                .progress(0)
                .errorMessage("任务不存在")
                .build();
    }

    // ========== 题库分页查询 ==========

    @Override
    public PageResultVO<BanksVO> pageQuery(String keyword, Integer page, Integer pageSize) {
        // ===== 参数兜底 =====
        long current = (page == null || page < 1) ? 1 : page;
        long size = (pageSize == null || pageSize < 1) ? 100 : Math.min(pageSize, MAX_PAGE_SIZE);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        // ===== 第 1 步：分页查题库（只查当前页）=====
        LambdaQueryWrapper<Bank> wrapper = Wrappers.<Bank>lambdaQuery()
                .and(hasKeyword, w -> w.like(Bank::getName, keyword).or().like(Bank::getFileName, keyword))
                .orderByDesc(Bank::getCreatedAt);           // 新导入的排前面

        Page<Bank> pageParam = Page.of(current, size);//创建分页参数
        bankMapper.selectPage(pageParam, wrapper);          // MP 自动执行 COUNT + LIMIT
        List<Bank> banks = pageParam.getRecords();

        if (banks.isEmpty()) {
            return new PageResultVO<>(List.of(), pageParam.getTotal(), current, size);
        }

        // ===== 第 2 步：统计这一页题库的题目数（按题型）和章节数 =====
        List<Long> bankIds = banks.stream().map(Bank::getId).toList();

        // 2.1 题目数：一次查完这一页所有题库 → bankId → (题型 → 数量)
        Map<Long, Map<String, Integer>> typeCountMap = new HashMap<>();
        for (BankCountDTO row : questionMapper.countGroupByBankAndType(bankIds)) {
            Long bankId = row.getBankId();

            // 先看这个题库有没有内层 Map，没有就建一个放进外层
            Map<String, Integer> typeCounts = typeCountMap.get(bankId);
            if (typeCounts == null) {
                typeCounts = new HashMap<>();
                typeCountMap.put(bankId, typeCounts);
            }

            // 再往里放「题型 → 数量」
            typeCounts.put(row.getType(), row.getCnt());
        }

        // 2.2 章节数：一次查完 → bankId → 章节数
        Map<Long, Integer> chapterCountMap = new HashMap<>();
        for (BankCountDTO row : chapterMapper.countGroupByBank(bankIds)) {
            chapterCountMap.put(row.getBankId(), row.getCnt());
        }

        // ===== 第 3 步：组装 VO 列表 =====
        List<BanksVO> list = new ArrayList<>(banks.size());
        for (Bank bank : banks) {
            list.add(toBankVO(bank,
                    typeCountMap.getOrDefault(bank.getId(), Map.of()),
                    chapterCountMap.getOrDefault(bank.getId(), 0)));
        }

        // ===== 第 4 步：返回 list + 总记录数 + 页码 + 每页记录数 =====
        return new PageResultVO<>(list, pageParam.getTotal(), current, size);
    }

    // ========== 题库详情 ==========

    @Override
    public BankDetailVO getBankDetail(Long id) {
        // 1) 题库本身：不存在直接返回 null，由 Controller 翻译成 40401
        Bank bank = bankMapper.selectById(id);
        if (bank == null) {
            return null;
        }

        // 2) 题型统计：复用分页那边的 Mapper 方法（只传一个 id）
        Map<String, Integer> typeCounts = new HashMap<>();
        for (BankCountDTO row : questionMapper.countGroupByBankAndType(List.of(id))) {
            typeCounts.put(row.getType(), row.getCnt());
        }

        // 3) 章节 + 每章题目数（一条 JOIN 查询搞定，已按 sort 排好序）
        List<ChapterVO> chapters = chapterMapper.selectChaptersWithCount(id);

        // 4) 组装：公共字段交给 fillBankFields，再补详情特有的两个
        BankDetailVO detail = new BankDetailVO();
        fillBankFields(detail, bank, typeCounts, chapters.size());
        detail.setChapters(chapters);
        detail.setWrongCount(0);          // TODO 错题功能做完后，改成查错题表
        return detail;
    }

    @Override
    public void deleteBank(Long id) {
        // 1) 先找出这个题库对应的导入任务，用来定位磁盘上的源文件
        //    （文件存成 uploads/{taskId}.{ext}，所以要拿 taskId 和原始文件名里的扩展名）
        List<BankImport> tasks = bankImportMapper.selectList(
                Wrappers.<BankImport>lambdaQuery().eq(BankImport::getBankId, id));

        // 2) 删题库：chapter / question 由外键 ON DELETE CASCADE 自动清理
        bankMapper.deleteById(id);

        // 3) 再删磁盘上的源文件
        //    顺序讲究：先保证数据库删掉（业务正确），再删文件；
        //    文件系统不受事务保护，删失败只记日志，不影响接口返回
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        for (BankImport task : tasks) {
            String storedName = task.getTaskId() + "." + extOf(task.getFileName());
            deleteQuietly(dir.resolve(storedName));
        }
    }

    // ========== 组装辅助 ==========

    /** 列表用：题库实体 + 统计结果 → 列表项 VO */
    private BanksVO toBankVO(Bank bank, Map<String, Integer> typeCounts, int chapterCount) {
        BanksVO vo = new BanksVO();
        fillBankFields(vo, bank, typeCounts, chapterCount);
        return vo;
    }

    /**
     * 填充"列表项"和"详情"共用的字段。
     * 参数类型是父类 BanksVO —— 子类 BankDetailVO 也能直接传进来，所以两边共用这一份逻辑。
     */
    private void fillBankFields(BanksVO vo, Bank bank, Map<String, Integer> typeCounts, int chapterCount) {
        vo.setId(bank.getId());
        vo.setName(bank.getName());
        vo.setFileName(bank.getFileName());
        vo.setFileType(bank.getFileType());
        vo.setCreatedAt(bank.getCreatedAt());
        vo.setChapterCount(chapterCount);

        // 各题型数量：直接按题型 key 取值，这个题库没有该题型就是 0
        // （这些字符串就是 QuestionType 的枚举名）
        vo.setSingleCount(typeCounts.getOrDefault("SINGLE", 0));
        vo.setMultipleCount(typeCounts.getOrDefault("MULTIPLE", 0));
        vo.setTrueFalseCount(typeCounts.getOrDefault("TRUE_FALSE", 0));
        vo.setEssayCount(typeCounts.getOrDefault("ESSAY", 0));
        vo.setFillBlankCount(typeCounts.getOrDefault("FILL_BLANK", 0));

        // 题目总数：Map 里没有"总数"这一项，把所有题型的数量加起来
        // （认不出的题型也会被算进来，总数不会漏）
        int questionCount = typeCounts.values().stream()
                .mapToInt(cnt -> cnt == null ? 0 : cnt)
                .sum();
        vo.setQuestionCount(questionCount);
    }

    // ========== 小工具 ==========

    /**
     * 把异常压成一句人话（≤200 字）。
     * 完整堆栈只进日志；数据库/前端只需要一个简短、可读的原因
     * （也避免把 SQL 报错和表结构泄露出去）。
     */
    private String briefReason(Throwable e) {
        String raw = e.getMessage();
        if (raw == null || raw.isBlank()) {
            raw = e.getClass().getSimpleName();
        }
        String firstLine = raw.lines().findFirst().orElse(raw).trim();
        return firstLine.length() > MAX_REASON_LENGTH ? firstLine.substring(0, MAX_REASON_LENGTH) : firstLine;
    }

    /** 删文件失败不影响主流程，所以吞掉异常只记日志 */
    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("清理落盘文件失败：{}", file, e);
        }
    }

    /** 取小写扩展名；没有扩展名返回空串 */
    private String extOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}
