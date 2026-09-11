package com.asteria.pojo.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题库 VO。
 *
 * <p>两个接口共用：
 * <ul>
 *   <li>上传 / 导入任务：只用 taskId、status；</li>
 *   <li>题库分页查询：用下面的题库字段（题型数量都是查出来的，不落库）。</li>
 * </ul>
 */
@Data
public class BanksVO {

    // ===== 导入任务用 =====
    private String taskId;
    private String status;

    // ===== 题库列表用 =====
    private Long id;
    private String name;
    private String fileName;
    private String fileType;
    private Integer questionCount;
    private Integer singleCount;
    private Integer multipleCount;
    private Integer trueFalseCount;
    private Integer essayCount;
    private Integer fillBlankCount;
    private Integer chapterCount;
    private LocalDateTime createdAt;
}
