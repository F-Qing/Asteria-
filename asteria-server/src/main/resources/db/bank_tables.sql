-- ============================================================
-- 题库三表：bank（题库）→ chapter（章节）→ question（题目）
-- 关系：import_task.bank_id → bank.id
-- 用法：mysql -uroot -p finaltext < bank_tables.sql
-- ============================================================

USE finaltext;

-- 题库表：一次成功导入产出 1 个题库
CREATE TABLE IF NOT EXISTS bank (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题库ID（自增，对应接口里的 number id）',
  name       VARCHAR(100) NOT NULL COMMENT '题库名称：上传时填的 bankName，缺省取文件名',
  file_name  VARCHAR(255) DEFAULT NULL COMMENT '来源文件名',
  file_type  VARCHAR(10)  DEFAULT NULL COMMENT '来源文件类型：docx/pdf/txt',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库表';

-- 章节表：题库下的分组；文件里没有章节时为“默认章节”
CREATE TABLE IF NOT EXISTS chapter (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  bank_id    BIGINT       NOT NULL COMMENT '所属题库ID',
  name       VARCHAR(200) NOT NULL COMMENT '章节名；文件里没有章节时为“默认章节”',
  sort       INT          NOT NULL DEFAULT 0 COMMENT '排序（前端按此升序展示）',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_chapter_bank (bank_id, sort),
  CONSTRAINT fk_chapter_bank FOREIGN KEY (bank_id) REFERENCES bank (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='章节表';

-- 题目表：bank_id 冗余一列，方便按题库直接筛题（故不建外键）
CREATE TABLE IF NOT EXISTS question (
  id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  bank_id          BIGINT      NOT NULL COMMENT '所属题库ID（冗余，方便按题库筛题）',
  chapter_id       BIGINT      NOT NULL COMMENT '所属章节ID',
  type             VARCHAR(20) NOT NULL COMMENT '题型：SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK',
  stem             TEXT        NOT NULL COMMENT '题干',
  `options`        JSON        DEFAULT NULL COMMENT '选项数组 [{"key":"A","content":"..."}]，判断/填空题/简答题为 NULL',
  answer           TEXT        NOT NULL COMMENT '标准答案：单选A / 多选A,B / 判断TRUE|FALSE / 简答文本 / 填空多个空用；分隔',
  analysis         TEXT        DEFAULT NULL COMMENT '答案解析（现在留空，接AI后填）',
  knowledge_points JSON        DEFAULT NULL COMMENT '知识点标签数组（接AI后填）',
  created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_question_bank_chapter (bank_id, chapter_id),
  KEY idx_question_bank_type (bank_id, type),
  CONSTRAINT fk_question_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目表';
