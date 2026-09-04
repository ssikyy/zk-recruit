-- =============================================================
-- 熵基科技轻量招聘系统 数据库结构
-- 对应需求文档 V1.3 第 14 章
-- 说明：JSON 类内容统一使用 LONGTEXT 存储，不使用数据库 JSON 函数，
--       保证可移植性（应用层从不在 JSON 内部做查询）。
--       candidate_id 恒等于 sys_user.id（角色为 CANDIDATE 的用户）。
-- =============================================================

CREATE DATABASE IF NOT EXISTS zk_recruit
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE zk_recruit;

SET FOREIGN_KEY_CHECKS = 0;

-- V1.3 已移除首页内容管理，重建时清掉历史表
DROP TABLE IF EXISTS site_asset;
DROP TABLE IF EXISTS site_content;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS interview_info;
DROP TABLE IF EXISTS application_log;
DROP TABLE IF EXISTS job_application;
DROP TABLE IF EXISTS job;
DROP TABLE IF EXISTS job_location;
DROP TABLE IF EXISTS job_category;
DROP TABLE IF EXISTS resume_file;
DROP TABLE IF EXISTS resume;
DROP TABLE IF EXISTS candidate_profile;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------------------------------------------
-- 账号与角色（§5、§14）
-- -------------------------------------------------------------
CREATE TABLE sys_user
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(120) NOT NULL COMMENT '登录邮箱，全局唯一',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希，禁止明文',
    name          VARCHAR(50)  NOT NULL COMMENT '显示姓名',
    role          VARCHAR(20)  NOT NULL COMMENT 'CANDIDATE / HR',
    hr_admin      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '管理员 HR 权限位',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
    last_login_at DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    KEY idx_role_status (role, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '账号';

-- -------------------------------------------------------------
-- 求职者基本资料（§9.1）
-- -------------------------------------------------------------
CREATE TABLE candidate_profile
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL COMMENT '= sys_user.id',
    name       VARCHAR(50) NOT NULL,
    phone      VARCHAR(20)  NULL COMMENT '投递前必填，格式 ^1[3-9]\\d{9}$',
    gender     VARCHAR(20)  NULL COMMENT 'MALE / FEMALE / UNKNOWN',
    city       VARCHAR(50)  NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '求职者基本资料';

-- -------------------------------------------------------------
-- 在线简历（可变，§9.2）
-- -------------------------------------------------------------
CREATE TABLE resume
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    candidate_id BIGINT   NOT NULL,
    content      LONGTEXT NULL COMMENT '在线简历 JSON',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_candidate (candidate_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '在线简历';

-- -------------------------------------------------------------
-- 附件简历（不可变、多版本，§9.3）
-- -------------------------------------------------------------
CREATE TABLE resume_file
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    candidate_id BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_key  VARCHAR(300) NOT NULL COMMENT '相对存储路径，禁止对外暴露',
    file_size    BIGINT       NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    is_current   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '当前附件简历标记',
    uploaded_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_candidate_current (candidate_id, is_current)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '附件简历（不可变多版本）';

-- -------------------------------------------------------------
-- 字典：职位类别、工作地点（§10.7）
-- -------------------------------------------------------------
CREATE TABLE job_category
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(20) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    status     VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cat_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '职位类别字典';

CREATE TABLE job_location
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(20) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    status     VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_loc_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '工作地点字典';

-- -------------------------------------------------------------
-- 职位（§10.3、§12.1）
-- -------------------------------------------------------------
CREATE TABLE job
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    title            VARCHAR(120) NOT NULL,
    recruitment_type VARCHAR(20)  NOT NULL COMMENT 'SOCIAL / CAMPUS',
    category_id      BIGINT       NOT NULL,
    location_id      BIGINT       NOT NULL,
    owner_hr_id      BIGINT       NOT NULL COMMENT '职位负责人',
    headcount        INT          NOT NULL DEFAULT 1,
    education        VARCHAR(20)  NOT NULL COMMENT '学历要求',
    experience       VARCHAR(30)  NULL COMMENT '社招必填',
    graduation_year  VARCHAR(20)  NULL COMMENT '校招必填',
    target_audience  VARCHAR(20)  NULL COMMENT '校招必填 GRADUATE / INTERN',
    duty             TEXT         NOT NULL COMMENT '岗位职责',
    requirement      TEXT         NOT NULL COMMENT '任职要求',
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PUBLISHED / CLOSED',
    version          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁 + 快照版本',
    published_at     DATETIME     NULL,
    created_by       BIGINT       NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_job_query (recruitment_type, status, published_at),
    KEY idx_job_owner (owner_hr_id, status),
    KEY idx_job_category (category_id),
    KEY idx_job_location (location_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '职位';

-- -------------------------------------------------------------
-- 投递记录（含快照，§13.2）
-- -------------------------------------------------------------
CREATE TABLE job_application
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    candidate_id       BIGINT      NOT NULL,
    job_id             BIGINT      NOT NULL,
    attempt_no         INT         NOT NULL DEFAULT 1 COMMENT '第 N 次投递，上限 3',
    status             VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    version            INT         NOT NULL DEFAULT 0 COMMENT '乐观锁',
    applied_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at       DATETIME    NULL,
    last_handled_at    DATETIME    NULL,
    resume_snapshot    LONGTEXT    NULL COMMENT '在线简历快照，写入后不可改',
    resume_file_id     BIGINT      NULL COMMENT '附件快照，指向不可变的 resume_file',
    job_snapshot       LONGTEXT    NOT NULL COMMENT '职位快照',
    candidate_snapshot LONGTEXT    NOT NULL COMMENT '联系信息快照',
    hr_note            TEXT        NULL COMMENT '内部备注，仅 HR 可见',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cand_job_att (candidate_id, job_id, attempt_no),
    KEY idx_app_status (status, applied_at),
    KEY idx_app_candidate (candidate_id, job_id),
    KEY idx_app_job (job_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '投递记录';

-- -------------------------------------------------------------
-- 投递操作日志（§12.2）
-- -------------------------------------------------------------
CREATE TABLE application_log
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    application_id BIGINT       NOT NULL,
    action         VARCHAR(40)  NOT NULL,
    from_status    VARCHAR(20)  NULL,
    to_status      VARCHAR(20)  NULL,
    operator_id    BIGINT       NOT NULL,
    operator_type  VARCHAR(20)  NOT NULL COMMENT 'CANDIDATE / HR',
    remark         VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_application (application_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '投递状态变更记录';

-- -------------------------------------------------------------
-- 面试安排与评价（单轮，1:1，§10.6）
-- -------------------------------------------------------------
CREATE TABLE interview_info
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    application_id BIGINT       NOT NULL,
    interview_time DATETIME     NOT NULL,
    method         VARCHAR(20)  NOT NULL COMMENT 'ONLINE / OFFLINE',
    address        VARCHAR(300) NOT NULL COMMENT '地点或会议链接',
    contact_note   VARCHAR(500) NULL,
    evaluation     TEXT         NULL COMMENT '仅 HR 可见',
    result         VARCHAR(20)  NULL COMMENT 'PASS / FAIL，空表示未出结果',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_application (application_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '面试信息';

-- -------------------------------------------------------------
-- 管理类操作审计（§16.2）
-- -------------------------------------------------------------
CREATE TABLE sys_operation_log
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    operator_id BIGINT      NOT NULL,
    module      VARCHAR(40) NOT NULL,
    action      VARCHAR(40) NOT NULL,
    target_id   VARCHAR(60) NULL,
    detail      LONGTEXT    NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operator (operator_id, id),
    KEY idx_module (module, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '管理操作审计日志';
