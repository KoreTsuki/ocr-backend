/*
 Navicat Premium Data Transfer

 Source Server         : 本机
 Source Server Type    : MySQL
 Source Server Version : 80034 (8.0.34)
 Source Host           : localhost:3306
 Source Schema         : ocr-db

 Target Server Type    : MySQL
 Target Server Version : 80034 (8.0.34)
 File Encoding         : 65001

 Date: 11/05/2024 00:02:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ocr_result
-- ----------------------------
DROP TABLE IF EXISTS `ocr_result`;
CREATE TABLE `ocr_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
  `image_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '图片链接',
  `text_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT 'ocr识别的文字结果',
  `audit_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '人工审核后的文本',
  `audit_status` tinyint NULL DEFAULT 0 COMMENT '审核状态 0-待审核 1-审核通过 2-人工修补',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人id',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `is_delete` tinyint NULL DEFAULT NULL COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ocr_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `ocr_audit_log`;
CREATE TABLE `ocr_audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `result_id` bigint NOT NULL COMMENT 'OCR结果id',
  `user_id` bigint NOT NULL COMMENT '结果所属用户id',
  `reviewer_id` bigint NOT NULL COMMENT '审核人id',
  `before_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '修改前文本',
  `after_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '修改后文本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_result_id` (`result_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_ocr_task
-- ----------------------------
DROP TABLE IF EXISTS `sys_ocr_task`;
CREATE TABLE `sys_ocr_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '任务ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '文件名',
  `file_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '文件URL',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '任务状态',
  `error_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始处理时间',
  `complete_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `queue_position` int NULL DEFAULT NULL COMMENT '入队位置',
  `consumer_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '消费者节点标识',
  `execute_duration_ms` bigint NULL DEFAULT NULL COMMENT '执行耗时ms',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_task_id` (`task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_file_record
-- ----------------------------
DROP TABLE IF EXISTS `sys_file_record`;
CREATE TABLE `sys_file_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '文件MD5',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '文件名',
  `minio_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'MinIO地址',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '内容类型',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_file_hash` (`file_hash`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ocr_result
-- ----------------------------


-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `openid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '微信openid',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '密码',
  `lines` bigint NULL DEFAULT NULL COMMENT '额度',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (2, 'oqJEX6BhRAeeh0CbUS1ipURfjdso','admin', '123456',8887);

SET FOREIGN_KEY_CHECKS = 1;
