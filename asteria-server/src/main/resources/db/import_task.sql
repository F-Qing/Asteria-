-- ============================================================
-- 导入任务表：import_task（一次上传一行，供进度轮询与失败原因落库）
-- 关系：import_task.bank_id → bank.id（不建外键：删题库时保留任务记录）
-- 用法：mysql -uroot -p finaltext < import_task.sql
-- ============================================================

USE finaltext;

CREATE TABLE IF NOT EXISTS import_task (
  task_id       VARCHAR(36)  NOT NULL COMMENT '任务ID：应用生成的UUID。主键=应用发号（取代自增的位置）',
  status        VARCHAR(20)  NOT NULL COMMENT 'PENDING/PARSING/AI_PROCESSING/SUCCESS/FAILED。决策点2裁决：VARCHAR不用ENUM——将来加状态只改Java枚举，ENUM却要ALTER表，修改成本不对称',
  file_name     VARCHAR(255) NOT NULL COMMENT '用户上传的原始文件名。它还兼职一份工：末尾的扩展名让解析线程能定位 uploads/{task_id}.{ext}',
  file_size     BIGINT       NOT NULL COMMENT '文件大小（字节），对应Java Long',
  bank_name     VARCHAR(255) DEFAULT NULL COMMENT '目标题库名称：上传时前端传入，缺省取文件名；M3 建题库时写入 bank.name',
  progress      INT          NOT NULL DEFAULT '0' COMMENT '进度0-100（M3由后台线程按阶段权重推进）',
  bank_id       BIGINT       DEFAULT NULL COMMENT '成功后生成的题库ID。决策点3裁决：可空——契约明说"未完成时为null"，可空性编码状态机语义',
  total_count   INT          NOT NULL DEFAULT '0' COMMENT '识别题目总数，解析阶段填入',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因。可空——契约明说"成功时为null"',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入任务表：任务的一生都在这张表里';
