package com.alibaba.csp.sentinel.dashboard.datasource.mapper;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.Metric;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MetricMapper
 *
 * @author 张朝阳
 * @since 2022-04-11 23:25:41
 */
@Mapper
public interface MetricMapper extends BaseMapper<Metric> {

    int batchInsert(List<Metric> metricList);
}
