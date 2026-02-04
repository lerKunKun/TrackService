-- MySQL dump 10.13  Distrib 9.5.0, for Linux (x86_64)
--
-- Host: localhost    Database: logistics_system
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '687b2b07-c50c-11f0-b2d2-ceca6abf917b:1-805';

--
-- Table structure for table `allowed_corp_ids`
--

DROP TABLE IF EXISTS `allowed_corp_ids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `allowed_corp_ids` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `corp_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '浼佷笟CorpId',
  `corp_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浼佷笟鍚嶇О',
  `status` tinyint DEFAULT '1' COMMENT '鐘舵€侊細1-鍚敤锛?-绂佺敤',
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍒涘缓浜?,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `deleted_at` datetime DEFAULT NULL COMMENT '杞垹闄ゆ椂闂?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `corp_id` (`corp_id`),
  UNIQUE KEY `corp_id_2` (`corp_id`),
  KEY `idx_corp_id` (`corp_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍏佽鐧诲綍鐨勪紒涓欳orpId鐧藉悕鍗?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `archive_logs`
--

DROP TABLE IF EXISTS `archive_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `archive_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏃ュ織ID',
  `archive_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '褰掓。绫诲瀷锛歵racking/events',
  `archived_count` int NOT NULL DEFAULT '0' COMMENT '褰掓。鏁伴噺',
  `days_threshold` int NOT NULL COMMENT '褰掓。澶╂暟闃堝€?,
  `started_at` datetime NOT NULL COMMENT '寮€濮嬫椂闂?,
  `completed_at` datetime DEFAULT NULL COMMENT '瀹屾垚鏃堕棿',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'running' COMMENT '鐘舵€侊細running/success/failed',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '閿欒淇℃伅',
  PRIMARY KEY (`id`),
  KEY `idx_started_at` (`started_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='褰掓。鏃ュ織琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '鏃ュ織ID',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '鎿嶄綔鐢ㄦ埛ID',
  `username` varchar(50) DEFAULT NULL COMMENT '鎿嶄綔鐢ㄦ埛鍚?,
  `operation` varchar(100) NOT NULL COMMENT '鎿嶄綔绫诲瀷',
  `module` varchar(50) NOT NULL COMMENT '妯″潡鍚嶇О',
  `method` varchar(200) DEFAULT NULL COMMENT '鏂规硶鍚?,
  `params` text COMMENT '璇锋眰鍙傛暟锛圝SON鏍煎紡锛?,
  `result` varchar(20) NOT NULL COMMENT '鎿嶄綔缁撴灉锛歋UCCESS/FAILURE',
  `error_msg` text COMMENT '閿欒淇℃伅',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP鍦板潃',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '鐢ㄦ埛浠ｇ悊',
  `execution_time` int DEFAULT NULL COMMENT '鎵ц鏃堕暱锛堟绉掞級',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_module` (`module`),
  KEY `idx_result` (`result`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瀹¤鏃ュ織琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carriers`
--

DROP TABLE IF EXISTS `carriers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carriers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `carrier_id` int NOT NULL COMMENT '17Track鎵胯繍鍟咺D',
  `carrier_code` varchar(50) NOT NULL COMMENT '绯荤粺鎵胯繍鍟嗕唬鐮?,
  `carrier_name` varchar(100) NOT NULL COMMENT '鎵胯繍鍟嗚嫳鏂囧悕绉?,
  `carrier_name_cn` varchar(100) DEFAULT NULL COMMENT '鎵胯繍鍟嗕腑鏂囧悕绉?,
  `country_id` int DEFAULT NULL COMMENT '鍥藉ID',
  `country_iso` varchar(10) DEFAULT NULL COMMENT '鍥藉ISO浠ｇ爜',
  `email` varchar(255) DEFAULT NULL COMMENT '鑱旂郴閭',
  `tel` varchar(100) DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽',
  `url` varchar(500) DEFAULT NULL COMMENT '瀹樼綉鍦板潃',
  `is_active` tinyint DEFAULT '1' COMMENT '鏄惁鍚敤',
  `sort_order` int DEFAULT '0' COMMENT '鎺掑簭',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `carrier_id` (`carrier_id`),
  KEY `idx_carrier_code` (`carrier_code`),
  KEY `idx_carrier_id` (`carrier_id`),
  KEY `idx_country_iso` (`country_iso`)
) ENGINE=InnoDB AUTO_INCREMENT=3033 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎵胯繍鍟嗘槧灏勮〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `demo_table`
--

DROP TABLE IF EXISTS `demo_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_table` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dingtalk_dept_mapping`
--

DROP TABLE IF EXISTS `dingtalk_dept_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dingtalk_dept_mapping` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `dingtalk_dept_id` bigint NOT NULL COMMENT '閽夐拤閮ㄩ棬ID',
  `system_dept_id` bigint NOT NULL COMMENT '绯荤粺閮ㄩ棬ID',
  `dingtalk_dept_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閽夐拤閮ㄩ棬鍚嶇О',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dingtalk_dept_id` (`dingtalk_dept_id`),
  KEY `idx_system_dept_id` (`system_dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閽夐拤閮ㄩ棬鏄犲皠琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dingtalk_sync_logs`
--

DROP TABLE IF EXISTS `dingtalk_sync_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dingtalk_sync_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `sync_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍚屾绫诲瀷锛欶ULL-鍏ㄩ噺锛孌EPT-閮ㄩ棬锛孶SER-鐢ㄦ埛锛孯OLE-瑙掕壊鏄犲皠',
  `sync_mode` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍚屾妯″紡锛歁ANUAL-鎵嬪姩锛孉UTO-鑷姩',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐘舵€侊細RUNNING-杩涜涓紝SUCCESS-鎴愬姛锛孎AILED-澶辫触',
  `total_count` int DEFAULT '0' COMMENT '鎬绘暟',
  `success_count` int DEFAULT '0' COMMENT '鎴愬姛鏁?,
  `failed_count` int DEFAULT '0' COMMENT '澶辫触鏁?,
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '閿欒淇℃伅',
  `started_at` datetime DEFAULT NULL COMMENT '寮€濮嬫椂闂?,
  `completed_at` datetime DEFAULT NULL COMMENT '瀹屾垚鏃堕棿',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閽夐拤鍚屾鏃ュ織琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fulfillments`
--

DROP TABLE IF EXISTS `fulfillments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fulfillments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '璁㈠崟ID',
  `shop_id` bigint NOT NULL COMMENT '搴楅摵ID',
  `shopify_fulfillment_id` bigint DEFAULT NULL COMMENT 'Shopify灞ョ害ID',
  `tracking_number` varchar(255) NOT NULL COMMENT '杩愬崟鍙?,
  `tracking_company` varchar(100) DEFAULT NULL COMMENT '鐗╂祦鍏徃',
  `tracking_url` varchar(500) DEFAULT NULL COMMENT '璺熻釜URL',
  `status` varchar(50) DEFAULT 'pending' COMMENT '鐘舵€?,
  `synced_to_shopify` tinyint(1) DEFAULT '0' COMMENT '鏄惁宸插悓姝ュ埌Shopify',
  `sync_error` text COMMENT '鍚屾閿欒淇℃伅',
  `synced_at` timestamp NULL DEFAULT NULL COMMENT '鍚屾鏃堕棿',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_tracking_number` (`tracking_number`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fulfillments_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='璁㈠崟灞ョ害琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liquid_schema_cache`
--

DROP TABLE IF EXISTS `liquid_schema_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liquid_schema_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '缂撳瓨ID',
  `theme_name` varchar(500) NOT NULL COMMENT '盲赂禄茅垄藴氓聬聧莽搂掳',
  `version` varchar(50) NOT NULL COMMENT '莽鈥八喢ε撀ヂ徛?,
  `file_path` varchar(500) NOT NULL COMMENT '忙鈥撯€∶ぢ宦睹仿ヂ锯€?,
  `section_name` varchar(100) DEFAULT NULL COMMENT 'Schema盲赂颅莽拧鈥瀗ame氓颅鈥斆β?,
  `section_type` varchar(100) DEFAULT NULL COMMENT 'Section莽卤禄氓啪鈥姑β犫€∶€?,
  `schema_json` text NOT NULL COMMENT '氓庐艗忙鈥⒙疵♀€瀞chema JSON',
  `settings_count` int DEFAULT '0' COMMENT 'Settings忙鈥⒙懊┾€÷?,
  `settings_hash` varchar(64) DEFAULT NULL COMMENT 'Settings莽拧鈥濵D5氓鈥溗喢ヂ概?,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '氓藛鈥好ヂ宦好︹€斅睹┾€斅?,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '忙鈥郝疵︹€撀懊︹€斅睹┾€斅?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_theme_version_file` (`theme_name`(200),`version`,`file_path`(200)),
  KEY `idx_section_name` (`section_name`),
  KEY `idx_hash` (`settings_hash`),
  KEY `idx_theme_version` (`theme_name`(255))
) ENGINE=InnoDB AUTO_INCREMENT=1137 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Liquid Schema缂撳瓨琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `menus`
--

DROP TABLE IF EXISTS `menus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menus` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '鑿滃崟ID',
  `parent_id` bigint unsigned DEFAULT '0' COMMENT '鐖惰彍鍗旾D锛?涓洪《绾ц彍鍗曪級',
  `menu_name` varchar(50) NOT NULL COMMENT '鑿滃崟鍚嶇О',
  `menu_code` varchar(50) NOT NULL COMMENT '鑿滃崟缂栫爜锛堝敮涓€鏍囪瘑锛?,
  `menu_type` varchar(20) NOT NULL COMMENT '鑿滃崟绫诲瀷锛歁ENU-鑿滃崟锛孊UTTON-鎸夐挳',
  `path` varchar(200) DEFAULT NULL COMMENT '璺敱璺緞',
  `component` varchar(200) DEFAULT NULL COMMENT '缁勪欢璺緞',
  `icon` varchar(50) DEFAULT NULL COMMENT '鑿滃崟鍥炬爣',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭锛堣秺灏忚秺闈犲墠锛?,
  `visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '鏄惁鏄剧ず锛?-闅愯棌锛?-鏄剧ず',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '鐘舵€侊細0-绂佺敤锛?-鍚敤',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=80 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鑿滃崟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '璁㈠崟ID',
  `shopify_line_item_id` bigint DEFAULT NULL COMMENT 'Shopify琛岄」鐩甀D',
  `sku` varchar(255) DEFAULT NULL COMMENT 'SKU',
  `title` varchar(500) NOT NULL COMMENT '鍟嗗搧鏍囬',
  `variant_title` varchar(255) DEFAULT NULL COMMENT '鍙樹綋鏍囬',
  `quantity` int NOT NULL COMMENT '鏁伴噺',
  `price` decimal(10,2) NOT NULL COMMENT '鍗曚环',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku` (`sku`),
  CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='璁㈠崟鏄庣粏琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL COMMENT '搴楅摵ID',
  `shopify_order_id` bigint NOT NULL COMMENT 'Shopify璁㈠崟ID',
  `order_number` varchar(50) NOT NULL COMMENT '璁㈠崟鍙凤紙濡?001锛?,
  `order_name` varchar(50) DEFAULT NULL COMMENT '璁㈠崟鍚嶇О锛堝#1001锛?,
  `customer_email` varchar(255) DEFAULT NULL COMMENT '瀹㈡埛閭',
  `customer_name` varchar(255) DEFAULT NULL COMMENT '瀹㈡埛濮撳悕',
  `customer_phone` varchar(50) DEFAULT NULL COMMENT '瀹㈡埛鐢佃瘽',
  `total_price` decimal(10,2) NOT NULL COMMENT '鎬婚噾棰?,
  `currency` varchar(10) NOT NULL COMMENT '璐у竵',
  `financial_status` varchar(50) DEFAULT NULL COMMENT '鏀粯鐘舵€?,
  `fulfillment_status` varchar(50) DEFAULT NULL COMMENT '灞ョ害鐘舵€?,
  `shipping_address_name` varchar(200) DEFAULT NULL COMMENT '鏀惰揣浜哄鍚?,
  `shipping_address_address1` varchar(255) DEFAULT NULL COMMENT '鍦板潃琛?',
  `shipping_address_city` varchar(100) DEFAULT NULL COMMENT '鍩庡競',
  `shipping_address_province` varchar(100) DEFAULT NULL COMMENT '鐪?宸?,
  `shipping_address_country` varchar(100) DEFAULT NULL COMMENT '鍥藉',
  `shipping_address_zip` varchar(20) DEFAULT NULL COMMENT '閭紪',
  `shipping_address_phone` varchar(50) DEFAULT NULL COMMENT '鐢佃瘽',
  `created_at` timestamp NULL DEFAULT NULL COMMENT 'Shopify璁㈠崟鍒涘缓鏃堕棿',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT 'Shopify璁㈠崟鏇存柊鏃堕棿',
  `synced_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍚屾鏃堕棿',
  `raw_data` json DEFAULT NULL COMMENT '瀹屾暣璁㈠崟JSON鏁版嵁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_shopify_order` (`shop_id`,`shopify_order_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_order_number` (`order_number`),
  KEY `idx_customer_email` (`customer_email`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='璁㈠崟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `parcels`
--

DROP TABLE IF EXISTS `parcels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parcels` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍖呰９ID',
  `order_id` bigint DEFAULT NULL COMMENT '璁㈠崟ID',
  `parcel_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍖呰９缂栧彿',
  `carrier_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗕唬鐮?,
  `carrier_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗗悕绉?,
  `shipped_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'in_transit',
  `parcel_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍖呰９鍙?,
  `weight` decimal(8,2) DEFAULT NULL COMMENT '閲嶉噺(kg)',
  `length` decimal(8,2) DEFAULT NULL COMMENT '闀垮害(cm)',
  `width` decimal(8,2) DEFAULT NULL COMMENT '瀹藉害(cm)',
  `height` decimal(8,2) DEFAULT NULL COMMENT '楂樺害(cm)',
  `parcel_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍖呰９鐘舵€?,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `fk_parcels_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍖呰９琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '鏉冮檺ID',
  `permission_name` varchar(100) NOT NULL COMMENT '鏉冮檺鍚嶇О',
  `permission_code` varchar(100) NOT NULL COMMENT '鏉冮檺缂栫爜锛堝敮涓€鏍囪瘑锛?,
  `permission_type` varchar(20) NOT NULL COMMENT '鏉冮檺绫诲瀷锛歁ENU-鑿滃崟锛孊UTTON-鎸夐挳锛孌ATA-鏁版嵁',
  `resource_type` varchar(50) DEFAULT NULL COMMENT '璧勬簮绫诲瀷',
  `resource_id` bigint DEFAULT NULL COMMENT '璧勬簮ID锛堝鑿滃崟ID锛?,
  `description` varchar(200) DEFAULT NULL COMMENT '鏉冮檺鎻忚堪',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '鐘舵€侊細0-绂佺敤锛?-鍚敤',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏉冮檺琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_images` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍥剧墖ID',
  `product_id` bigint NOT NULL COMMENT '浜у搧ID',
  `variant_id` bigint DEFAULT NULL COMMENT '氓鈥β趁佲€澝♀€灻ヂ徦溍ぢ解€淚D(氓聫炉茅鈧€?',
  `src` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '氓鈥郝久р€扳€RL',
  `alt_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '忙鈥郝棵ぢ宦Ｃ︹€撯€∶ε撀?,
  `position` int DEFAULT '0' COMMENT '鍥剧墖浣嶇疆椤哄簭',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_variant_id` (`variant_id`),
  KEY `idx_position` (`position`),
  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_product_images_variant` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浜у搧鍥剧墖琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_imports`
--

DROP TABLE IF EXISTS `product_imports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_imports` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '氓炉录氓鈥βッ懊ヂ解€D',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '氓炉录氓鈥βッ︹€撯€∶ぢ宦睹ヂ惵?,
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '忙鈥撯€∶ぢ宦睹ヂ溍モ€毬仿ヂ锯€?,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '氓炉录氓鈥βッ犅睹︹偓聛: 0-猫驴鈥好∨捗ぢ嘎? 1-忙藛聬氓艩鸥, 2-氓陇卤猫麓楼',
  `total_records` int DEFAULT '0' COMMENT '忙鈧幻懊ヂ解€⒚︹€⒙?,
  `success_records` int DEFAULT '0' COMMENT '忙藛聬氓艩鸥氓炉录氓鈥βッ懊ヂ解€⒚︹€⒙?,
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '茅鈥濃劉猫炉炉盲驴隆忙聛炉',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '氓藛鈥好ヂ宦好︹€斅睹┾€斅?,
  `completed_at` datetime DEFAULT NULL COMMENT '氓庐艗忙藛聬忙鈥斅睹┾€斅?,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浜у搧瀵煎叆璁板綍琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_shops`
--

DROP TABLE IF EXISTS `product_shops`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_shops` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍏宠仈ID',
  `product_id` bigint NOT NULL COMMENT '浜у搧ID',
  `shop_id` bigint NOT NULL COMMENT '鍟嗗簵ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `publish_status` tinyint DEFAULT '0' COMMENT '??????? 0-????? 1-?????CSV), 2-?????API???), 3-???',
  `last_export_time` datetime DEFAULT NULL COMMENT '???????SV???',
  `published_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_shop` (`product_id`,`shop_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_published_by` (`published_by`),
  CONSTRAINT `fk_product_shops_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_product_shops_shop` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浜у搧鍟嗗簵鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_template_suffixes`
--

DROP TABLE IF EXISTS `product_template_suffixes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_template_suffixes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `theme_name` varchar(100) NOT NULL,
  `suffix` varchar(50) NOT NULL,
  `sections_snapshot` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_theme_suffix` (`theme_name`,`suffix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Product Template Suffixes';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_variants`
--

DROP TABLE IF EXISTS `product_variants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_variants` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '氓聫藴盲陆鈥淚D',
  `product_id` bigint NOT NULL COMMENT '浜у搧ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍙樹綋鏍囬',
  `price` decimal(10,2) DEFAULT NULL COMMENT '浠锋牸',
  `compare_at_price` decimal(10,2) DEFAULT NULL COMMENT '鍘熶环',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '氓聫藴盲陆鈥溍モ€郝久р€扳€RL',
  `inventory_quantity` int DEFAULT '0' COMMENT '氓潞鈥溍ヂ溍︹€⒙懊┾€÷?,
  `weight` decimal(10,2) DEFAULT NULL COMMENT '茅鈥÷嵜┾€÷?氓鈥︹€?',
  `barcode` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '忙聺隆氓陆垄莽聽聛',
  `option1_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閫夐」1鍚嶇О(濡? Color)',
  `option1_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閫夐」1鍊?濡? Red)',
  `option2_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閫夐」2鍚嶇О(濡? Size)',
  `option2_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閫夐」2鍊?濡? Large)',
  `option3_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '茅鈧€懊┞÷?氓聬聧莽搂掳(氓娄鈥? Material)',
  `option3_value` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '茅鈧€懊┞÷?氓鈧?氓娄鈥? Cotton)',
  `sku` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SKU搴撳瓨鍗曚綅',
  `procurement_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '茅鈥♀€∶绰┾€溌久ε铰?,
  `procurement_price` decimal(10,2) DEFAULT NULL COMMENT '茅鈥♀€∶绰ぢ宦访β犅?,
  `supplier` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '茅鈥♀€∶绰モ€⑩€犆ヂ惵嵜?,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '氓藛鈥好ヂ宦好︹€斅睹┾€斅?,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '忙鈥郝疵︹€撀懊︹€斅睹┾€斅?,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku` (`sku`),
  KEY `idx_supplier` (`supplier`),
  CONSTRAINT `fk_product_variants_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1648 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浜у搧鍙樹綋琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_visibility`
--

DROP TABLE IF EXISTS `product_visibility`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_visibility` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `role_id` bigint unsigned DEFAULT NULL,
  `shop_id` bigint DEFAULT NULL,
  `granted_by` bigint NOT NULL,
  `granted_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_user_shop` (`product_id`,`user_id`,`shop_id`),
  UNIQUE KEY `uk_product_role_shop` (`product_id`,`role_id`,`shop_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_product_visibility_lookup` (`product_id`,`user_id`,`role_id`,`shop_id`),
  CONSTRAINT `product_visibility_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `product_visibility_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `product_visibility_ibfk_3` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `product_visibility_ibfk_4` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浜у搧ID',
  `handle` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '浜у搧鍞竴鏍囪瘑绗?鐢ㄤ簬URL)',
  `title` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '浜у搧鏍囬',
  `body_html` text COLLATE utf8mb4_unicode_ci COMMENT '浜у搧鎻忚堪(HTML鏍煎紡)',
  `vendor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍝佺墝/鍒堕€犲晢',
  `tags` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏍囩(閫楀彿鍒嗛殧)',
  `procurement_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '茅鈥♀€∶绰┾€溌久ε铰?,
  `procurement_price` decimal(10,2) DEFAULT NULL COMMENT '茅鈥♀€∶绰ぢ宦访β犅?,
  `supplier` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '茅鈥♀€∶绰モ€⑩€犆ヂ惵嵜?,
  `published` tinyint DEFAULT '0' COMMENT '涓婃灦鐘舵€?0-鑽夌 1-宸蹭笂鏋?,
  `product_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '盲潞搂氓鈥溌伱ニ喤犆р劉禄茅鈥溌久ε铰?P3盲陆驴莽鈥澛?',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '氓藛鈥好ヂ宦好︹€斅睹┾€斅?,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '忙鈥郝疵︹€撀懊︹€斅睹┾€斅?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_handle` (`handle`),
  KEY `idx_title` (`title`),
  KEY `idx_published` (`published`),
  KEY `idx_vendor` (`vendor`),
  KEY `idx_supplier` (`supplier`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浜у搧涓昏〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_mapping_rules`
--

DROP TABLE IF EXISTS `role_mapping_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_mapping_rules` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '瑙勫垯鍚嶇О',
  `rule_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '瑙勫垯绫诲瀷锛欴EPT-鎸夐儴闂紝TITLE-鎸夎亴浣嶏紝DEPT_TITLE-閮ㄩ棬+鑱屼綅',
  `dingtalk_dept_id` bigint DEFAULT NULL COMMENT '閽夐拤閮ㄩ棬ID锛坮ule_type涓篋EPT鎴朌EPT_TITLE鏃跺繀濉級',
  `dingtalk_title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閽夐拤鑱屼綅鍚嶇О锛坮ule_type涓篢ITLE鎴朌EPT_TITLE鏃跺繀濉級',
  `role_id` bigint unsigned NOT NULL COMMENT '绯荤粺瑙掕壊ID锛堝叧鑱攔oles琛級',
  `priority` int DEFAULT '0' COMMENT '浼樺厛绾э紙鏁板瓧瓒婂ぇ浼樺厛绾ц秺楂橈級',
  `status` tinyint(1) DEFAULT '1' COMMENT '鐘舵€侊細0-绂佺敤锛?-鍚敤',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_system_role_id` (`role_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='瑙掕壊鏄犲皠瑙勫垯琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_menus`
--

DROP TABLE IF EXISTS `role_menus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_menus` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint unsigned NOT NULL COMMENT '瑙掕壊ID',
  `menu_id` bigint unsigned NOT NULL COMMENT '鑿滃崟ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瑙掕壊鑿滃崟鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint unsigned NOT NULL COMMENT '瑙掕壊ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '鏉冮檺ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=327 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瑙掕壊鏉冮檺鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '瑙掕壊ID',
  `role_name` varchar(50) NOT NULL COMMENT '瑙掕壊鍚嶇О',
  `role_code` varchar(50) NOT NULL COMMENT '瑙掕壊缂栫爜锛堝敮涓€鏍囪瘑锛?,
  `description` varchar(200) DEFAULT NULL COMMENT '瑙掕壊鎻忚堪',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '鐘舵€侊細0-绂佺敤锛?-鍚敤',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瑙掕壊琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shops`
--

DROP TABLE IF EXISTS `shops`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shops` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍟嗗簵ID',
  `user_id` bigint NOT NULL COMMENT '鎵€灞炵敤鎴稩D',
  `shop_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '搴楅摵鍚嶇О',
  `platform` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '骞冲彴绫诲瀷锛歴hopify, shopline, tiktok',
  `shop_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '搴楅摵URL',
  `shop_domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Shopify搴楅摵鍩熷悕(xxx.myshopify.com)',
  `timezone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '搴楅摵鏃跺尯',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API瀵嗛挜',
  `api_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API瀵嗛挜Secret',
  `access_token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璁块棶浠ょ墝',
  `token_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'offline' COMMENT 'Token绫诲瀷锛歰ffline(姘镐箙), online(24灏忔椂)',
  `connection_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '杩炴帴鐘舵€侊細active(姝ｅ父), invalid(澶辨晥), pending(寰呮巿鏉?',
  `last_validated_at` datetime DEFAULT NULL COMMENT '鏈€鍚庨獙璇佹椂闂?,
  `webhook_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Webhook瀵嗛挜',
  `oauth_state` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OAuth state nonce',
  `oauth_scope` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'OAuth鎺堟潈鑼冨洿',
  `token_expires_at` datetime DEFAULT NULL COMMENT 'Token杩囨湡鏃堕棿',
  `is_active` tinyint NOT NULL DEFAULT '1' COMMENT '鏄惁婵€娲伙細0-鍚︼紝1-鏄?,
  `last_sync_time` datetime DEFAULT NULL COMMENT '鏈€鍚庡悓姝ユ椂闂?,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `deleted_at` datetime DEFAULT NULL COMMENT '鍒犻櫎鏃堕棿锛堣蒋鍒犻櫎锛?,
  `contact_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍟嗗簵鑱旂郴閭',
  `owner_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '搴椾富閭',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍟嗗簵璐у竵浠ｇ爜',
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璁㈤槄璁″垝鍚嶇О',
  `plan_display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璁″垝鏄剧ず鍚嶇О',
  `is_shopify_plus` tinyint(1) DEFAULT '0' COMMENT '鏄惁涓篠hopify Plus',
  `primary_domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '涓诲煙鍚?,
  `shop_owner` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '搴椾富濮撳悕',
  `iana_timezone` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IANA鏃跺尯鏍囪瘑',
  `shop_info_json` text COLLATE utf8mb4_unicode_ci COMMENT '瀹屾暣鍟嗗簵淇℃伅JSON锛堢敤浜庢墿灞曞瓨鍌級',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_shop_domain` (`shop_domain`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_platform` (`platform`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_deleted_at` (`deleted_at`),
  KEY `idx_shops_currency` (`currency`),
  KEY `idx_shops_plan_name` (`plan_name`),
  CONSTRAINT `fk_shops_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搴楅摵琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sku_applications`
--

DROP TABLE IF EXISTS `sku_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sku_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐢宠ID',
  `product_variant_id` bigint NOT NULL COMMENT '盲潞搂氓鈥溌伱ヂ徦溍ぢ解€淚D',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '莽鈥澛趁访犅睹︹偓聛: 0-氓戮鈥γヂ∶β犅? 1-氓路虏茅鈧∶库€? 2-氓路虏忙鈥光€櫭宦?,
  `generated_sku` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '莽鈥澟该λ喡惷♀€濻KU',
  `applied_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '莽鈥澛趁访︹€斅睹┾€斅?,
  `reviewed_at` datetime DEFAULT NULL COMMENT '氓庐隆忙聽赂忙鈥斅睹┾€斅?,
  PRIMARY KEY (`id`),
  KEY `idx_variant_id` (`product_variant_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_sku_applications_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU鐢宠琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sync_jobs`
--

DROP TABLE IF EXISTS `sync_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sync_jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浠诲姟ID',
  `job_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '浠诲姟绫诲瀷锛歰rder_sync, tracking_sync',
  `shop_id` bigint DEFAULT NULL COMMENT '搴楅摵ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '浠诲姟鐘舵€侊細pending, running, completed, failed',
  `total_count` int DEFAULT '0' COMMENT '鎬绘暟',
  `success_count` int DEFAULT '0' COMMENT '鎴愬姛鏁?,
  `failed_count` int DEFAULT '0' COMMENT '澶辫触鏁?,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '閿欒淇℃伅',
  `started_at` datetime DEFAULT NULL COMMENT '寮€濮嬫椂闂?,
  `completed_at` datetime DEFAULT NULL COMMENT '瀹屾垚鏃堕棿',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍚屾浠诲姟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `theme_exports`
--

DROP TABLE IF EXISTS `theme_exports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `theme_exports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `theme_name` varchar(100) NOT NULL,
  `version` varchar(20) NOT NULL,
  `export_path` varchar(500) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `exported_by` varchar(50) DEFAULT NULL,
  `exported_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_theme_name` (`theme_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Theme Exports';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `theme_migration_history`
--

DROP TABLE IF EXISTS `theme_migration_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `theme_migration_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `theme_name` varchar(100) NOT NULL,
  `from_version` varchar(20) NOT NULL,
  `to_version` varchar(20) NOT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `templates_updated` int DEFAULT '0',
  `executed_by` varchar(50) DEFAULT NULL,
  `executed_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `error_message` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_theme_name` (`theme_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Theme Migration History';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `theme_migration_rules`
--

DROP TABLE IF EXISTS `theme_migration_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `theme_migration_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '盲赂禄茅鈥澛甀D',
  `theme_name` varchar(100) NOT NULL COMMENT '盲赂禄茅垄藴氓聬聧莽搂掳',
  `from_version` varchar(50) NOT NULL COMMENT '婧愮増鏈彿',
  `to_version` varchar(50) NOT NULL COMMENT '鐩爣鐗堟湰鍙?,
  `rule_type` varchar(50) NOT NULL COMMENT '瑙勫垯绫诲瀷',
  `section_name` varchar(100) DEFAULT NULL COMMENT '猫艩鈥毭β得ヂ惵嵜?,
  `rule_json` text NOT NULL COMMENT 'JSON忙聽录氓录聫莽拧鈥灻р€灻ニ嗏劉猫炉娄忙茠鈥?,
  `confidence` varchar(20) DEFAULT NULL COMMENT '莽陆庐盲驴隆氓潞娄',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '氓藛鈥好ヂ宦好︹€斅睹┾€斅?,
  `created_by` varchar(100) DEFAULT NULL COMMENT '氓藛鈥好ヂ宦好ぢ郝?,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '忙鈥郝疵︹€撀懊︹€斅睹┾€斅?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule` (`theme_name`,`from_version`,`to_version`,`rule_type`,`section_name`),
  KEY `idx_version` (`theme_name`,`from_version`,`to_version`),
  KEY `idx_type` (`rule_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1871 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='涓婚杩佺Щ瑙勫垯琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `theme_version_archive`
--

DROP TABLE IF EXISTS `theme_version_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `theme_version_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `theme_name` varchar(100) NOT NULL,
  `version` varchar(20) NOT NULL,
  `zip_file_path` varchar(500) NOT NULL,
  `zip_file_size` bigint DEFAULT NULL,
  `sections_count` int DEFAULT NULL,
  `is_current` tinyint(1) DEFAULT '0',
  `uploaded_by` varchar(50) DEFAULT NULL,
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_theme_version` (`theme_name`,`version`),
  KEY `idx_theme_name` (`theme_name`),
  KEY `idx_version` (`version`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Theme Version Archive';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tracking_events`
--

DROP TABLE IF EXISTS `tracking_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracking_events` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浜嬩欢ID',
  `tracking_id` bigint NOT NULL COMMENT '杩愬崟ID',
  `event_time` datetime NOT NULL COMMENT '浜嬩欢鏃堕棿',
  `event_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '浜嬩欢鎻忚堪',
  `event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浜嬩欢鍦扮偣',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍩庡競',
  `postal_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閭紪',
  `provider_key` int DEFAULT NULL COMMENT '鎵胯繍鍟咺D',
  `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗗悕绉?,
  `event_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浜嬩欢浠ｇ爜',
  `stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '闃舵(PickedUp/InTransit/Delivered绛?',
  `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀛愮姸鎬?,
  `time_iso` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ISO鏃堕棿瀛楃涓?,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_event_time` (`event_time`),
  KEY `idx_tracking_event_time` (`tracking_id`,`event_time` DESC) COMMENT '杩愬崟浜嬩欢澶嶅悎绱㈠紩-浼樺寲鏌ヨ鍜屾帓搴?,
  KEY `idx_stage` (`stage`) COMMENT '鐗╂祦闃舵绱㈠紩-鐢ㄤ簬闃舵绛涢€?,
  KEY `idx_tracking_stage` (`tracking_id`,`stage`) COMMENT '杩愬崟闃舵澶嶅悎绱㈠紩-鐢ㄤ簬鐗瑰畾杩愬崟闃舵鏌ヨ',
  CONSTRAINT `fk_events_tracking` FOREIGN KEY (`tracking_id`) REFERENCES `tracking_numbers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=814 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐗╂祦浜嬩欢琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tracking_events_history`
--

DROP TABLE IF EXISTS `tracking_events_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracking_events_history` (
  `id` bigint NOT NULL COMMENT '浜嬩欢ID',
  `tracking_id` bigint NOT NULL COMMENT '杩愬崟ID',
  `event_time` datetime NOT NULL COMMENT '浜嬩欢鏃堕棿',
  `event_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '浜嬩欢鎻忚堪',
  `event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浜嬩欢鍦扮偣',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍩庡競',
  `postal_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閭紪',
  `provider_key` int DEFAULT NULL COMMENT '鎵胯繍鍟咺D',
  `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗗悕绉?,
  `event_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浜嬩欢浠ｇ爜',
  `stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '闃舵',
  `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀛愮姸鎬?,
  `time_iso` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ISO鏃堕棿瀛楃涓?,
  `created_at` datetime NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '褰掓。鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_tracking_id` (`tracking_id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍘嗗彶鐗╂祦浜嬩欢琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tracking_numbers`
--

DROP TABLE IF EXISTS `tracking_numbers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracking_numbers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '杩愬崟ID',
  `user_id` bigint DEFAULT NULL,
  `parcel_id` bigint DEFAULT NULL COMMENT '鍖呰９ID锛堝彲涓虹┖锛屾墜鍔ㄦ坊鍔犵殑杩愬崟锛?,
  `tracking_number` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '杩愬崟鍙?,
  `carrier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗕唬鐮?,
  `carrier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗗悕绉?,
  `carrier_id` int DEFAULT NULL COMMENT '17Track鎵胯繍鍟咺D',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '澶囨敞',
  `track_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璁㈠崟鐘舵€?,
  `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀛愮姸鎬?,
  `sub_status_descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀛愮姸鎬佹弿杩?,
  `days_of_transit` int DEFAULT NULL COMMENT '杩愯緭澶╂暟',
  `days_after_last_update` int DEFAULT NULL COMMENT '璺濈鏈€鍚庢洿鏂板ぉ鏁?,
  `latest_event_time` datetime DEFAULT NULL COMMENT '鏈€鏂颁簨浠舵椂闂?,
  `latest_event_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏈€鏂颁簨浠舵弿杩?,
  `latest_event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏈€鏂颁簨浠跺湴鐐?,
  `pickup_time` datetime DEFAULT NULL COMMENT '鎻芥敹鏃堕棿',
  `delivered_time` datetime DEFAULT NULL COMMENT '绛炬敹鏃堕棿',
  `last_sync_at` datetime DEFAULT NULL,
  `next_sync_at` datetime DEFAULT NULL,
  `raw_status` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `destination_country` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐩殑鍦板浗瀹?,
  `origin_country` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '濮嬪彂鍥藉浠ｇ爜',
  `package_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍖呰９鐘舵€?,
  `track17_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track鐘舵€佺爜',
  `track17_substatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track瀛愮姸鎬佺爜',
  `latest_event` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '鏈€鏂扮墿娴佷簨浠?,
  `registered_at_17track` tinyint DEFAULT '0' COMMENT '鏄惁宸叉敞鍐屽埌17Track锛?-鍚︼紝1-鏄?,
  `last_sync_time` datetime DEFAULT NULL COMMENT '鏈€鍚庡悓姝ユ椂闂?,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `version` int NOT NULL DEFAULT '0' COMMENT '鐗堟湰鍙凤紙涔愯閿侊級',
  `deleted_at` datetime DEFAULT NULL COMMENT '鍒犻櫎鏃堕棿锛堣蒋鍒犻櫎锛?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tracking_number` (`tracking_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parcel_id` (`parcel_id`),
  KEY `idx_carrier_code` (`carrier_code`),
  KEY `idx_package_status` (`package_status`),
  KEY `idx_track_status` (`track_status`) COMMENT '鐘舵€佺储寮?鐢ㄤ簬鐘舵€佺瓫閫?,
  KEY `idx_created_at` (`created_at`) COMMENT '鍒涘缓鏃堕棿绱㈠紩-鐢ㄤ簬鏃ユ湡鑼冨洿鏌ヨ',
  KEY `idx_updated_at` (`updated_at` DESC) COMMENT '鏇存柊鏃堕棿绱㈠紩-鐢ㄤ簬鎺掑簭',
  KEY `idx_status_updated` (`track_status`,`updated_at` DESC) COMMENT '鐘舵€?鏃堕棿澶嶅悎绱㈠紩-瑕嗙洊鐘舵€佺瓫閫?鎺掑簭',
  KEY `idx_carrier_updated` (`carrier_code`,`updated_at` DESC) COMMENT '鎵胯繍鍟?鏃堕棿澶嶅悎绱㈠紩-瑕嗙洊鎵胯繍鍟嗙瓫閫?鎺掑簭',
  KEY `idx_user_status_updated` (`user_id`,`track_status`,`updated_at` DESC) COMMENT '鐢ㄦ埛+鐘舵€?鏃堕棿澶嶅悎绱㈠紩-澶氱鎴锋煡璇紭鍖?,
  KEY `idx_next_sync` (`next_sync_at`) COMMENT '涓嬫鍚屾鏃堕棿绱㈠紩-瀹氭椂浠诲姟鏌ヨ浼樺寲',
  KEY `idx_source` (`source`) COMMENT '鏉ユ簮绱㈠紩-鐢ㄤ簬鏉ユ簮绛涢€?,
  KEY `idx_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_tracking_parcel` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_tracking_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杩愬崟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tracking_numbers_history`
--

DROP TABLE IF EXISTS `tracking_numbers_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracking_numbers_history` (
  `id` bigint NOT NULL COMMENT '杩愬崟ID',
  `user_id` bigint DEFAULT NULL,
  `parcel_id` bigint DEFAULT NULL COMMENT '鍖呰９ID',
  `tracking_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '杩愬崟鍙?,
  `carrier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗕唬鐮?,
  `carrier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵胯繍鍟嗗悕绉?,
  `carrier_id` int DEFAULT NULL COMMENT '17Track鎵胯繍鍟咺D',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '澶囨敞',
  `track_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀛愮姸鎬?,
  `sub_status_descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀛愮姸鎬佹弿杩?,
  `days_of_transit` int DEFAULT NULL COMMENT '杩愯緭澶╂暟',
  `days_after_last_update` int DEFAULT NULL COMMENT '璺濈鏈€鍚庢洿鏂板ぉ鏁?,
  `latest_event_time` datetime DEFAULT NULL COMMENT '鏈€鏂颁簨浠舵椂闂?,
  `latest_event_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏈€鏂颁簨浠舵弿杩?,
  `latest_event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏈€鏂颁簨浠跺湴鐐?,
  `pickup_time` datetime DEFAULT NULL COMMENT '鎻芥敹鏃堕棿',
  `delivered_time` datetime DEFAULT NULL COMMENT '绛炬敹鏃堕棿',
  `last_sync_at` datetime DEFAULT NULL,
  `next_sync_at` datetime DEFAULT NULL,
  `raw_status` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `destination_country` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐩殑鍦板浗瀹?,
  `origin_country` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '濮嬪彂鍥藉浠ｇ爜',
  `package_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍖呰９鐘舵€?,
  `track17_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track鐘舵€佺爜',
  `track17_substatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track瀛愮姸鎬佺爜',
  `latest_event` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '鏈€鏂扮墿娴佷簨浠?,
  `registered_at_17track` tinyint DEFAULT '0' COMMENT '鏄惁宸叉敞鍐屽埌17Track',
  `last_sync_time` datetime DEFAULT NULL COMMENT '鏈€鍚庡悓姝ユ椂闂?,
  `created_at` datetime NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL COMMENT '鏇存柊鏃堕棿',
  `version` int NOT NULL DEFAULT '0' COMMENT '鐗堟湰鍙?,
  `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '褰掓。鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_tracking_number` (`tracking_number`),
  KEY `idx_delivered_time` (`delivered_time`),
  KEY `idx_archived_at` (`archived_at`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍘嗗彶杩愬崟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tracking_webhook_logs`
--

DROP TABLE IF EXISTS `tracking_webhook_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracking_webhook_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tracking_id` bigint DEFAULT NULL,
  `payload` json DEFAULT NULL,
  `received_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tracking_id` (`tracking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐗╂祦Webhook鏃ュ織琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint unsigned NOT NULL COMMENT '鐢ㄦ埛ID',
  `role_id` bigint unsigned NOT NULL COMMENT '瑙掕壊ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢ㄦ埛瑙掕壊鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_shop_roles`
--

DROP TABLE IF EXISTS `user_shop_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_shop_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `role_id` bigint unsigned NOT NULL,
  `granted_by` bigint DEFAULT NULL,
  `granted_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_shop_role` (`user_id`,`shop_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_expires_at` (`expires_at`),
  CONSTRAINT `user_shop_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_shop_roles_ibfk_2` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_shop_roles_ibfk_3` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢ㄦ埛鍚?,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀵嗙爜锛圔Crypt鍔犲瘑锛?,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閭',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐢佃瘽',
  `real_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐪熷疄濮撳悕',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT '瑙掕壊锛欰DMIN, USER',
  `status` tinyint DEFAULT '1' COMMENT '鐘舵€侊細1-鍚敤锛?-绂佺敤',
  `sync_enabled` tinyint(1) DEFAULT '1' COMMENT '鏄惁鍚敤鑷姩鍚屾锛?-绂佺敤锛?-鍚敤',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '澶村儚URL',
  `last_login_time` datetime DEFAULT NULL COMMENT '鏈€鍚庣櫥褰曟椂闂?,
  `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏈€鍚庣櫥褰旾P',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `deleted_at` datetime DEFAULT NULL COMMENT '杞垹闄ゆ椂闂?,
  `ding_union_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閽夐拤UnionID',
  `ding_userid` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閽夐拤userId锛堜紒涓氬唴鍞竴鏍囪瘑锛?,
  `job_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '宸ュ彿',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鑱屼綅',
  `ding_user_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閽夐拤userId锛堜紒涓氬唴鍞竴鏍囪瘑锛?,
  `corp_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浼佷笟CorpId',
  `login_source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PASSWORD' COMMENT '鐧诲綍鏉ユ簮锛歅ASSWORD, DINGTALK',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `username_2` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted_at` (`deleted_at`),
  KEY `idx_ding_union_id` (`ding_union_id`),
  KEY `idx_corp_id` (`corp_id`),
  KEY `idx_login_source` (`login_source`),
  KEY `idx_ding_userid` (`ding_userid`)
) ENGINE=InnoDB AUTO_INCREMENT=299 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢ㄦ埛琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `webhook_logs`
--

DROP TABLE IF EXISTS `webhook_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `webhook_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏃ュ織ID',
  `shop_id` bigint DEFAULT NULL COMMENT '搴楅摵ID',
  `webhook_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Webhook绫诲瀷',
  `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '璇锋眰浣擄紙JSON锛?,
  `headers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '璇锋眰澶达紙JSON锛?,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '澶勭悊鐘舵€侊細success, failed',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '閿欒淇℃伅',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Webhook鏃ュ織琛?;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-03  2:57:32
