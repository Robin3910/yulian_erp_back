-- 订单列表
-- `ruoyi-vue-pro`.temu_order definition
CREATE TABLE `temu_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `product_title` varchar(500) NOT NULL COMMENT '商品标题',
  `order_status` tinyint(4) NOT NULL COMMENT '订单状态',
  `sku` varchar(64) NOT NULL COMMENT 'SKU编号',
  `skc` varchar(64) NOT NULL COMMENT 'SKC编号',
  `sale_price` decimal(10,2) NOT NULL COMMENT '申报价格',
  `custom_sku` varchar(64) DEFAULT NULL COMMENT '定制SKU',
  `quantity` int(11) NOT NULL COMMENT '数量',
  `product_properties` text COMMENT '商品属性',
  `booking_time` datetime DEFAULT NULL COMMENT '预定单创建时间',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `custom_image_urls` text COMMENT '定制图片列表URL',
  `custom_text_list` text COMMENT '定制文字列表',
  `product_img_url` text COMMENT '商品图片URL',
  `category_id` varchar(64) DEFAULT NULL COMMENT '类目ID',
  `category_name` varchar(255) DEFAULT NULL COMMENT '类目名称',
  `shipping_info` text COMMENT '物流信息JSON字符串',
  `original_info` text COMMENT '接口接收的源信息',
  `deleted` tinyint(1) DEFAULT '0',
  `creator` varchar(200) DEFAULT NULL,
  `updater` varchar(200) DEFAULT NULL,
  `tenant_id` int(11) unsigned DEFAULT '1',
  `effective_img_url` varchar(2000) DEFAULT NULL COMMENT '合成预览图',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单位价格',
  `total_price` decimal(10,2) DEFAULT NULL COMMENT '总价',
  `price_rule` text COMMENT '记录价格规则',
  PRIMARY KEY (`id`),
  KEY `idx_sku` (`sku`) COMMENT 'SKU查询索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺ID查询索引',
  KEY `custom_sku` (`custom_sku`) COMMENT '定制SKU查询索引',
  KEY `skc` (`skc`) COMMENT 'SKC查询索引',
  KEY `idx_category_id` (`category_id`) COMMENT '类目ID查询索引',
  KEY `idx_order_no` (`order_no`) COMMENT '订单编号索引'
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 商品品类表
CREATE TABLE `temu_product_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint(20) NOT NULL COMMENT '商品品类ID',
  `category_name` varchar(100) NOT NULL COMMENT '商品名称',
  `length` decimal(10,2) DEFAULT NULL COMMENT '长度(cm)',
  `width` decimal(10,2) DEFAULT NULL COMMENT '宽度(cm)',
  `height` decimal(10,2) DEFAULT NULL COMMENT '高度(cm)',
  `weight` decimal(10,2) DEFAULT NULL COMMENT '重量(g)',
  `main_image_url` varchar(1000) DEFAULT NULL COMMENT '主图URL',
  `unit_price` varchar(2000) DEFAULT NULL COMMENT '单价(JSON格式)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户编号';
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COMMENT='商品品类表';

-- 商品品类SKU关系表
CREATE TABLE `temu_product_category_sku` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint(20) NOT NULL COMMENT '商品品类ID',
  `category_name` varchar(100) NOT NULL COMMENT '商品名称',
  `sku` varchar(64) NOT NULL COMMENT 'SKU',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_sku` (`category_id`,`sku`),
  KEY `idx_sku` (`sku`) COMMENT 'SKU查询索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺ID查询索引',
  KEY `idx_shop_sku` (`shop_id`,`sku`) COMMENT '店铺ID和SKU联合索引'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='商品品类SKU关系表';

-- 商品表
CREATE TABLE `temu_product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint(20) NOT NULL COMMENT '商品品类',
  `sku` varchar(64) NOT NULL COMMENT 'SKU',
  `product_name` varchar(200) NOT NULL COMMENT '商品名称',
  `spu` varchar(64) NOT NULL COMMENT 'SPU',
  `skc` varchar(64) NOT NULL COMMENT 'SKC',
  `specifications` varchar(500) DEFAULT NULL COMMENT '规格',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sku` (`sku`) COMMENT 'SKU查询索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺ID查询索引',
  KEY `skc` (`skc`) COMMENT 'SKC查询索引',
  KEY `spu` (`spu`) COMMENT 'SPU查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 店铺表
CREATE TABLE `temu_shop` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺ID',
  `shop_name` varchar(100) NOT NULL COMMENT '店铺名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_id` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- 订单发货表
CREATE TABLE `temu_order_shipping_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `sku` varchar(64) NOT NULL COMMENT 'SKU',
  `express_image_url` varchar(500) NOT NULL COMMENT '快递面单图片URL',
  `express_outside_image_url` varchar(500) NOT NULL COMMENT '快递面单外单图片URL',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`) COMMENT '订单ID索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺ID索引',
  KEY `sku` (`sku`) COMMENT 'SKU索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单发货表';


-- 店铺SKC合规单URL表
CREATE TABLE `temu_shop_old_type_skc` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint(20) DEFAULT NULL COMMENT '店铺ID',
  `skc` varchar(255) NOT NULL COMMENT 'SKC编号',
  `old_type_url` varchar(255) DEFAULT NULL COMMENT '合规单URL',
  `old_type` varchar(64) DEFAULT NULL COMMENT '合规单年龄类型,\r\n0  0+,  1  3+,  3  14+',
  `create_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除状态 0 未删除 1已删除',
  `creator` varchar(255) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(255) DEFAULT NULL COMMENT '更新人',
  `tenant_id` bigint(20) NOT NULL DEFAULT '1' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_id_skc` (`shop_id`,`skc`) USING BTREE COMMENT '店铺id和skc唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4;