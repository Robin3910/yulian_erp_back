-- 订单列表
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
  `product_properties` varchar(500) DEFAULT NULL COMMENT '商品属性',
  `booking_time` datetime DEFAULT NULL COMMENT '预定单创建时间',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `custom_image_urls` varchar(2000) DEFAULT NULL COMMENT '定制图片列表URL',
  `custom_text_list` varchar(2000) DEFAULT NULL COMMENT '定制文字列表',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_sku` (`sku`) COMMENT 'SKU查询索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺ID查询索引',
  KEY `custom_sku` (`custom_sku`) COMMENT '定制SKU查询索引',
  KEY `skc` (`skc`) COMMENT 'SKC查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 商品品类表
CREATE TABLE `temu_product_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint(20) NOT NULL COMMENT '商品品类ID',
  `category_name` varchar(100) NOT NULL COMMENT '商品名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COMMENT='商品品类表';

-- 商品品类SKU关系表
CREATE TABLE `temu_product_category_sku` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint(20) NOT NULL COMMENT '商品品类ID',
  `sku` varchar(64) NOT NULL COMMENT 'SKU',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_sku` (`category_id`,`sku`),
  KEY `idx_sku` (`sku`) COMMENT 'SKU查询索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺ID查询索引'
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