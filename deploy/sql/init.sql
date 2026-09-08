-- 使用已有数据库
USE your_db;

-- 1. 奖状主表
CREATE TABLE `certificate` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `title` VARCHAR(200) NOT NULL COMMENT '奖状名称',
                               `recipient` VARCHAR(200) DEFAULT NULL COMMENT '获奖人员（多人用逗号分隔）',
                               `award_level` VARCHAR(20) DEFAULT NULL COMMENT '获奖级别：国/省/校/院',
                               `event_name` VARCHAR(200) DEFAULT NULL COMMENT '比赛赛事名称',
                               `project_name` VARCHAR(200) DEFAULT NULL COMMENT '获奖项目名称',
                               `organization` VARCHAR(100) DEFAULT NULL COMMENT '所属社团/学院',
                               `award_date` DATE DEFAULT NULL COMMENT '获奖时间',
                               `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
                               `sort_order` INT NOT NULL DEFAULT 0 COMMENT '置顶排序权重（越大越靠前）',
                               `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
                               `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-待审核删除，2-已隐藏',
                               `image_url` VARCHAR(512) DEFAULT NULL COMMENT '奖状图片访问URL',
                               `image_key` VARCHAR(256) DEFAULT NULL COMMENT 'OSS存储的文件Key（用于删除）',
                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               KEY `idx_award_level` (`award_level`),
                               KEY `idx_award_date` (`award_date`),
                               KEY `idx_status` (`status`),
                               KEY `idx_is_pinned_sort` (`is_pinned`, `sort_order`),
                               KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖状信息表';

-- 2. 管理员表
CREATE TABLE `admin` (
                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                         `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
                         `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
                         `role` VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '角色：SUPER_ADMIN-超级管理员，ADMIN-普通管理员',
                         `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
                         `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号表';

-- 3. 删除审核表
CREATE TABLE `delete_audit` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `certificate_id` BIGINT NOT NULL COMMENT '申请删除的奖状ID',
                                `certificate_title` VARCHAR(200) DEFAULT NULL COMMENT '提交时的奖状标题快照',
                                `requester_id` BIGINT NOT NULL COMMENT '申请管理员ID',
                                `requester_name` VARCHAR(50) DEFAULT NULL COMMENT '提交人姓名快照，提交人账号被删除后消息仍可显示',
                                `reason` VARCHAR(500) NOT NULL COMMENT '删除理由',
                                `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-同意删除，2-拒绝删除，3-超管直接删除',
                                `approver_id` BIGINT DEFAULT NULL COMMENT '审核管理员ID',
                                `approve_time` DATETIME DEFAULT NULL COMMENT '审核时间',
                                `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝理由',
                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_certificate_id` (`certificate_id`),
                                KEY `idx_status` (`status`),
                                KEY `idx_requester_id` (`requester_id`),
                                KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='删除审核记录表';

-- 4. 操作日志表
CREATE TABLE `operation_log` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `admin_id` BIGINT NOT NULL COMMENT '操作的管理员ID',
                                 `operation_type` VARCHAR(30) NOT NULL COMMENT '操作类型：ADD/UPDATE/DELETE_REQUEST/AUDIT/UPLOAD',
                                 `target_type` VARCHAR(30) DEFAULT NULL COMMENT '目标类型：Certificate/Admin/DeleteAudit',
                                 `target_id` BIGINT DEFAULT NULL COMMENT '目标对象ID',
                                 `detail` TEXT COMMENT '操作详情（可存JSON或描述）',
                                 `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '客户端IP地址',
                                 `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_admin_id` (`admin_id`),
                                 KEY `idx_operation_type` (`operation_type`),
                                 KEY `idx_target_id` (`target_id`),
                                 KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';