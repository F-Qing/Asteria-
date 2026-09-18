-- ============================================================
-- AI 聊天表
--   chat_session  会话
--   chat_message  消息（含流式中断）
--
-- 前置：无（这两张表不关联题库/章节 —— 会话不绑定题库，
--       需要题库内容时由 AI 通过"工具调用"自己去查，见后续 ChatService）
--
-- 用法：mysql -uroot -p finaltext < chat_tables.sql
--
-- 关系图：
--   chat_message.session_id → chat_session.id  【删会话 → 消息一起删】
-- ============================================================

-- ------------------------------------------------------------
-- 代码约定（DDL 表达不了，写 Service 时必须遵守）
--   1. message_count 是冗余字段：列表页要显示"共 N 条"，不冗余就得每行子查询。
--      每插入一条消息（user / assistant 都算）要 +1，删会话时不需要维护（整行一起删）。
--   2. updated_at 是"最近活动时间"：会话列表按它倒序，所以【每次发消息都要刷新它】，
--      否则聊了半天的会话还排在列表最后。
--   3. interrupted=1 表示"用户点了停止"：这时 content 里存的是【已经生成的那部分】，
--      不能丢 —— 丢了用户会觉得"我明明看到半句话，刷新就没了"。
-- ------------------------------------------------------------

USE finaltext;

-- ------------------------------------------------------------
-- 1. 会话表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_session (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  title         VARCHAR(100) NOT NULL DEFAULT '' COMMENT '标题（首条用户消息前20字自动生成，用户可改）',
  agent_mode    VARCHAR(20)  NOT NULL DEFAULT 'BUILTIN' COMMENT '模式：BUILTIN 内置模型 / EXTERNAL 外部',
  message_count INT          NOT NULL DEFAULT 0 COMMENT '消息条数（用户+AI 都算，冗余字段）',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近活动时间（列表按它倒序）',
  PRIMARY KEY (id),
  KEY idx_cs_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI聊天会话';

-- ------------------------------------------------------------
-- 2. 消息表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_message (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  session_id  BIGINT      NOT NULL COMMENT '所属会话ID',
  role        VARCHAR(16) NOT NULL COMMENT '角色：user 用户 / assistant AI / system 系统',
  content     MEDIUMTEXT  NOT NULL COMMENT '正文；流式中断时保存已生成的那部分',
  interrupted TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否被用户中途停止：0 否 / 1 是',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_cm_session_time (session_id, created_at),
  CONSTRAINT fk_cm_session FOREIGN KEY (session_id) REFERENCES chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI聊天消息';
