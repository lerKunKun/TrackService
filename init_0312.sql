/*
 Navicat Premium Data Transfer

 Source Server         : 17Track
 Source Server Type    : MySQL
 Source Server Version : 90500
 Source Host           : localhost:3306
 Source Schema         : logistics_system

 Target Server Type    : MySQL
 Target Server Version : 90500
 File Encoding         : 65001

 Date: 11/03/2026 18:33:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for product_visibility
-- ----------------------------
DROP TABLE IF EXISTS `product_visibility`;
CREATE TABLE `product_visibility`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `role_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '角色ID',
  `shop_id` bigint NULL DEFAULT NULL COMMENT '店铺ID',
  `granted_by` bigint NOT NULL COMMENT '授权人ID',
  `granted_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
  `expires_at` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_product_user_shop`(`product_id` ASC, `user_id` ASC, `shop_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_product_role_shop`(`product_id` ASC, `role_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_shop_id`(`shop_id` ASC) USING BTREE,
  INDEX `idx_expires_at`(`expires_at` ASC) USING BTREE,
  INDEX `idx_product_visibility_lookup`(`product_id` ASC, `user_id` ASC, `role_id` ASC, `shop_id` ASC) USING BTREE
);

SET FOREIGN_KEY_CHECKS = 1;
