-- =============================================================
-- 大乐透选号系统 - 数据库初始化脚本
-- 数据库：MySQL 8.x
-- 连接：172.16.206.128:3306  root / 123456
-- =============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS caipiao_dlt
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE caipiao_dlt;

-- 大乐透开奖号码表
CREATE TABLE IF NOT EXISTS dlt_number (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue       VARCHAR(16)  NOT NULL COMMENT '期号，如 26097',
    front1      TINYINT      NOT NULL COMMENT '前区第1码(1-35)',
    front2      TINYINT      NOT NULL COMMENT '前区第2码(1-35)',
    front3      TINYINT      NOT NULL COMMENT '前区第3码(1-35)',
    front4      TINYINT      NOT NULL COMMENT '前区第4码(1-35)',
    front5      TINYINT      NOT NULL COMMENT '前区第5码(1-35)',
    back1       TINYINT      NOT NULL COMMENT '后区第1码(1-12)',
    back2       TINYINT      NOT NULL COMMENT '后区第2码(1-12)',
    draw_date   DATE         NOT NULL COMMENT '开奖日期',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_issue (issue),
    KEY idx_draw_date (draw_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '大乐透历史开奖号码表';

-- 双色球开奖号码表
CREATE TABLE IF NOT EXISTS ssq_number (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue       VARCHAR(16)  NOT NULL COMMENT '期号，如 2026099',
    red1        TINYINT      NOT NULL COMMENT '红球第1码(1-33)',
    red2        TINYINT      NOT NULL COMMENT '红球第2码(1-33)',
    red3        TINYINT      NOT NULL COMMENT '红球第3码(1-33)',
    red4        TINYINT      NOT NULL COMMENT '红球第4码(1-33)',
    red5        TINYINT      NOT NULL COMMENT '红球第5码(1-33)',
    red6        TINYINT      NOT NULL COMMENT '红球第6码(1-33)',
    blue        TINYINT      NOT NULL COMMENT '蓝球号码(1-16)',
    draw_date   DATE         NOT NULL COMMENT '开奖日期',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_issue (issue),
    KEY idx_draw_date (draw_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '双色球历史开奖号码表';
