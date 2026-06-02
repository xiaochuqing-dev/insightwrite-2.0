-- ========================================
-- InsightWrite 2.0 DDL Script
-- MySQL 8.0 · InnoDB · utf8mb4
-- ========================================

CREATE DATABASE IF NOT EXISTS insightwrite
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE insightwrite;

-- ========================================
-- 表1: users（用户表）
-- ========================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id              INT             PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(120)    NOT NULL UNIQUE,
    password_hash   VARCHAR(256)    NOT NULL,
    avatar          TEXT,
    credits         INT             DEFAULT 600,
    credit_cycle_start DATETIME     NULL,
    last_credit_refill  DATETIME    NULL,
    last_login_at   DATETIME        NULL,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 表2: analysis_tasks（分析任务表）
-- ========================================
DROP TABLE IF EXISTS analysis_tasks;
CREATE TABLE analysis_tasks (
    id              INT             PRIMARY KEY AUTO_INCREMENT,
    user_id         INT             NOT NULL,
    essay_text      MEDIUMTEXT      NOT NULL,
    mode            VARCHAR(50)     DEFAULT '',
    custom_requirement VARCHAR(200) DEFAULT '',
    status          VARCHAR(20)     DEFAULT 'pending',
    result_text     MEDIUMTEXT,
    error_message   VARCHAR(500)    DEFAULT '',
    word_count      INT             DEFAULT 0,
    is_favorite     TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME        NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 表3: credit_transactions（积分流水表）
-- ========================================
DROP TABLE IF EXISTS credit_transactions;
CREATE TABLE credit_transactions (
    id              INT             PRIMARY KEY AUTO_INCREMENT,
    user_id         INT             NOT NULL,
    amount          INT             NOT NULL,
    reason          VARCHAR(100)    NOT NULL,
    balance_after   INT             NOT NULL,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 表4: articles（精选文章表）
-- ========================================
DROP TABLE IF EXISTS articles;
CREATE TABLE articles (
    id              INT             PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(200)    NOT NULL,
    content         MEDIUMTEXT      NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    source          VARCHAR(100)    DEFAULT '',
    word_count      INT             DEFAULT 0,
    view_count      INT             DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 表5: knowledge_entries（知识库表）
-- ========================================
DROP TABLE IF EXISTS knowledge_entries;
CREATE TABLE knowledge_entries (
    id              INT             PRIMARY KEY AUTO_INCREMENT,
    category        VARCHAR(50)     NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    content         MEDIUMTEXT      NOT NULL,
    tags            VARCHAR(200)    DEFAULT '',
    view_count      INT             DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_tags (tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 表6: favorites（收藏表）
-- ========================================
DROP TABLE IF EXISTS favorites;
CREATE TABLE favorites (
    id              INT             PRIMARY KEY AUTO_INCREMENT,
    user_id         INT             NOT NULL,
    task_id         INT             NOT NULL,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_task (user_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 表7: email_verification_codes（邮箱验证码表）
-- ========================================
DROP TABLE IF EXISTS email_verification_codes;
CREATE TABLE email_verification_codes (
    id              INT             PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(120)    NOT NULL,
    code            VARCHAR(10)     NOT NULL,
    purpose         VARCHAR(20)     NOT NULL,
    expires_at      DATETIME        NOT NULL,
    used            TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_purpose (email, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
