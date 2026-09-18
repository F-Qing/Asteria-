-- ============================================================
-- 考试倒计时表：textdatetime
-- 对应实体 com.asteria.pojo.entity.TextDateTime
-- 用法：mysql -uroot -p finaltext < textdatetime.sql
-- ============================================================

USE finaltext;

CREATE TABLE IF NOT EXISTS textdatetime (
  id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(100) NOT NULL COMMENT '考试名称',
  date DATE         NOT NULL COMMENT '考试日期',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试倒计时';
