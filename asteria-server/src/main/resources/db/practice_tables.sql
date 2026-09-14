-- ============================================================
-- 刷题练习四表
--   1. practice_session           刷题会话（一次「开始刷题」= 1 条）
--   2. practice_session_question  会话题目清单（含出题顺序）
--   3. practice_record            答题记录（一题在一次会话里的作答）
--   4. wrong_question             错题本（按题库去重，一道题一条）
--
-- 前置：必须先执行 bank_tables.sql（bank / chapter / question 已存在）
-- 用法：mysql -uroot -p finaltext < practice_tables.sql
--
-- 关系图：
--   practice_session.bank_id     → bank.id     【删题库 → 会话一起删】
--   practice_session.chapter_id  → chapter.id  【删章节 → 置 NULL，会话保留】
--   practice_session_question    → 会话 × 题目的中间表，sort 固定出题顺序
--   practice_record              → 会话 × 题目的作答（同题重答 = 覆盖）
--   wrong_question               → 题库 × 题目的错题本（唯一键去重）
--
-- 级联删除说明：删题库会连带删掉它的章节 → 题目 → 会话 → 记录 → 错题，
--              和前端删除确认弹窗的文案「级联删除题库及其章节、题目、刷题记录」一致。
-- ============================================================

-- ------------------------------------------------------------
-- 代码约定（DDL 表达不了，写 Service 时必须遵守，先记这儿免得忘）
--   1. 同题重答 → 覆盖 practice_record 那一行（靠 uk_pr_session_question 唯一键），
--      answered_count 只在「首次作答某题」时 +1，否则进度会超过 100%
--   2. 简答题 is_correct = NULL（现在没有 AI 判分）：不计入 correct_count，也不写入 wrong_question
--   3. 删题库 → 会话/记录/错题全部级联删除（对齐前端删除确认弹窗的文案）
-- ------------------------------------------------------------

USE finaltext;

-- ------------------------------------------------------------
-- 1. 刷题会话表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS practice_session (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  bank_id        BIGINT      NOT NULL COMMENT '所属题库ID',
  mode           VARCHAR(20) NOT NULL COMMENT '出题顺序：SEQUENTIAL 顺序 / RANDOM 随机',
  question_type  VARCHAR(20) NOT NULL COMMENT '题型：SINGLE/MULTIPLE/TRUE_FALSE/ESSAY/FILL_BLANK/ALL（ALL=全部题型）',
  chapter_id     BIGINT      DEFAULT NULL COMMENT '章节筛选；NULL = 全章节',
  status         VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS 进行中 / COMPLETED 已完成',
  total_count    INT         NOT NULL DEFAULT 0 COMMENT '题目总数（建会话时定下，之后不变）',
  answered_count INT         NOT NULL DEFAULT 0 COMMENT '已答题数（提交答案时 +1）',
  correct_count  INT         NOT NULL DEFAULT 0 COMMENT '答对数（提交答案且判对时 +1）',
  session_source VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '来源：NORMAL 普通刷题 / WRONG 错题重刷',
  completed_at   DATETIME    DEFAULT NULL COMMENT '完成时间（最后一题提交后写入）',
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_ps_bank_status (bank_id, status),
  CONSTRAINT fk_ps_bank FOREIGN KEY (bank_id) REFERENCES bank (id) ON DELETE CASCADE,
  CONSTRAINT fk_ps_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题会话表';

-- ------------------------------------------------------------
-- 2. 会话题目清单：会话里到底有哪几道题、按什么顺序出
--    为什么要单独一张表：随机模式（RANDOM）如果不把顺序存下来，
--    用户中途退出再进来，题目顺序就变了，已答记录也对不上。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS practice_session_question (
  id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id  BIGINT NOT NULL COMMENT '会话ID',
  question_id BIGINT NOT NULL COMMENT '题目ID',
  sort        INT    NOT NULL DEFAULT 0 COMMENT '第几题，从 1 开始（随机模式也固定下来）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_psq_session_question (session_id, question_id),
  KEY idx_psq_question (question_id),
  CONSTRAINT fk_psq_session FOREIGN KEY (session_id) REFERENCES practice_session (id) ON DELETE CASCADE,
  CONSTRAINT fk_psq_question FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话题目清单（含出题顺序）';

-- ------------------------------------------------------------
-- 3. 答题记录表
--    UNIQUE(session_id, question_id)：同一题在同一个会话里只留一行，
--    重答就覆盖（否则 answered_count 会重复计数、进度条会超过 100%）。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS practice_record (
  id          BIGINT     NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  session_id  BIGINT     NOT NULL COMMENT '所属会话ID',
  question_id BIGINT     NOT NULL COMMENT '题目ID',
  user_answer TEXT       NOT NULL COMMENT '用户作答：单选A / 多选ACD / 判断A=正确B=错误 / 填空多空用；分隔 / 简答为文本',
  is_correct  TINYINT(1) DEFAULT NULL COMMENT '是否正确：1 对 / 0 错 / NULL 未判分（简答等主观题）',
  created_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次作答时间',
  updated_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近作答时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pr_session_question (session_id, question_id),
  KEY idx_pr_question (question_id),
  CONSTRAINT fk_pr_session FOREIGN KEY (session_id) REFERENCES practice_session (id) ON DELETE CASCADE,
  CONSTRAINT fk_pr_question FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答题记录表';

-- ------------------------------------------------------------
-- 4. 错题本
--    按 (bank_id, question_id) 去重：同一道题错多次也只算 1 道错题，wrong_count 记次数。
--    resolved：错题重刷答对 → 置 1（不再计入「待攻克」，也不再被重刷选中）；
--              之后再答错 → 置回 0 且 wrong_count +1。记录永远保留，只是不再参与统计。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wrong_question (
  id             BIGINT     NOT NULL AUTO_INCREMENT COMMENT '错题ID',
  bank_id        BIGINT     NOT NULL COMMENT '所属题库ID（冗余，统计时不用每张表都 JOIN）',
  question_id    BIGINT     NOT NULL COMMENT '题目ID',
  user_answer    TEXT       NOT NULL COMMENT '最近一次的错误作答（错题回顾直接用）',
  wrong_count    INT        NOT NULL DEFAULT 1 COMMENT '累计答错次数',
  resolved       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已攻克：0 待攻克 / 1 已攻克',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wq_bank_question (bank_id, question_id),
  KEY idx_wq_bank_resolved (bank_id, resolved),
  KEY idx_wq_question (question_id),
  CONSTRAINT fk_wq_bank FOREIGN KEY (bank_id) REFERENCES bank (id) ON DELETE CASCADE,
  CONSTRAINT fk_wq_question FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本（按题库去重）';
