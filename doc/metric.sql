CREATE TABLE `metric` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `app` varchar(255) DEFAULT NULL COMMENT '应用名称',
    `resource` varchar(255) DEFAULT NULL COMMENT '资源名称',
    `timestamp` datetime DEFAULT NULL COMMENT '监控信息时间戳',
    `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
    `pass_qps` bigint(20) DEFAULT NULL COMMENT '通过QPS',
    `success_qps` bigint(20) DEFAULT NULL COMMENT '成功QPS',
    `block_qps` bigint(20) DEFAULT NULL COMMENT '限流QPS',
    `exception_qps` bigint(20) DEFAULT NULL COMMENT '异常QPS',
    `rt` decimal(10,2) DEFAULT NULL COMMENT '资源的平均响应时间',
    `count` int(10) DEFAULT NULL COMMENT '本次聚合的总条数',
    `resource_code` int(10) DEFAULT NULL COMMENT '资源hashcode',
    PRIMARY KEY (`id`),
    KEY `idx_app_timestamp` (`app`,`timestamp`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sentinel监控信息表';