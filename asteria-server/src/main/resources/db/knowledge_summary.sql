-- ============================================================
-- 知识点总结表
--   knowledge_summary  题库知识点总结（一个题库一份，AI 生成后缓存复用）
--
-- 前置：必须先执行 bank_tables.sql（bank 已存在）
-- 用法：mysql -uroot -p finaltext < knowledge_summary.sql
--
-- 关系图：
--   knowledge_summary.bank_id → bank.id  【删题库 → 总结一起删】
--
-- 为什么一张表就够：总结是「题库级」的，不是「用户级」也不是「会话级」——
-- POST /knowledge-summary 生成一次就存下来，之后 force=false 直接命中缓存返回结果，
-- 不需要每个用户各存一份（AI 配置是 BYOK，不落库，更不能跟着总结存）。
-- ============================================================

-- ------------------------------------------------------------
-- 代码约定（DDL 表达不了，写 Service 时必须遵守，先记这儿免得忘）
--   1. 一个题库只留一行：靠 uk_ks_bank 唯一键保证。重新生成（force=true）走 UPDATE，
--      不要先删后插 —— 那样 id 会变、首次生成时间也丢了。
--   2. 五个 JSON 列存的是「字符串数组」，写库前必须用 ObjectMapper.writeValueAsString()，
--      手写字符串会报 Invalid JSON text（question.options 已经踩过一次）。
--      实体用 String 承接，由 Service 层用 Jackson 转 List<String>。
--   3. generatedAt 取自 updated_at（不是 created_at）：updated_at 有 ON UPDATE，
--      force=true 重新生成时会刷新，正好就是「最近一次生成时间」。
--   4. bankName 不落库：题库改名字后总结里的旧名字会对不上，查询时 JOIN bank 取。
--   5. 命中缓存时别改这张表 —— fromCache=true 是返回给前端看的标记，不写库。
-- ------------------------------------------------------------

USE finaltext;

-- ------------------------------------------------------------
-- 1. 知识点总结表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_summary (
  id                BIGINT  NOT NULL AUTO_INCREMENT COMMENT '总结ID',
  bank_id           BIGINT  NOT NULL COMMENT '所属题库ID（一个题库一份）',
  highlights        JSON    NOT NULL COMMENT '重点摘要（字符串数组）',
  key_points        JSON    NOT NULL COMMENT '核心知识点（字符串数组）',
  hot_topics        JSON    NOT NULL COMMENT '高频考点（字符串数组）',
  easy_mistakes     JSON    NOT NULL COMMENT '易错点（字符串数组）',
  study_suggestions JSON    NOT NULL COMMENT '学习建议（字符串数组）',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次生成时间',
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间（= 对外返回的 generatedAt）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ks_bank (bank_id),
  CONSTRAINT fk_ks_bank FOREIGN KEY (bank_id) REFERENCES bank (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识点总结表（一个题库一份，AI 生成后可缓存复用）';
