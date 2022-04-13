/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.Metric;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.mapper.MetricMapper;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Save metrics data to database.
 *
 * @author 张朝阳
 */
@Component
public class InDatabaseMetricsRepository implements MetricsRepository<MetricEntity> {

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Resource
    private MetricMapper metricMapper;

    @Override
    public void save(MetricEntity entity) {
        if (entity == null || StringUtil.isBlank(entity.getApp())) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            metricMapper.insert(toPo(entity));
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            List<Metric> metricList = new ArrayList<>();
            metrics.forEach(metric -> metricList.add(toPo(metric)));
            metricMapper.batchInsert(metricList);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource,
                                                           long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<>();

        if (StringUtil.isBlank(app)) {
            return results;
        }

        readWriteLock.readLock().lock();
        try {
            Metric metric = new Metric();
            metric.setApp(app);
            metric.setResource(resource);
            QueryWrapper<Metric> queryWrapper = new QueryWrapper<>(metric);
            queryWrapper.between("timestamp", new Date(startTime), new Date(endTime));
            List<Metric> metricList = metricMapper.selectList(queryWrapper);

            if (CollectionUtils.isEmpty(metricList)) {
                return results;
            }

            metricList.forEach(e -> results.add(toPo(e)));
            return results;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }

        final long minTimeMs = System.currentTimeMillis() - 1000 * 60;
        Map<String, MetricEntity> resourceCount = new ConcurrentHashMap<>(32);

        readWriteLock.readLock().lock();
        try {
            QueryWrapper<Metric> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("app", app);
            queryWrapper.ge("timestamp", new Date(minTimeMs));
            List<Metric> metricList = metricMapper.selectList(queryWrapper);

            List<MetricEntity> metricEntityList = new ArrayList<>();
            metricList.forEach(e -> metricEntityList.add(toPo(e)));

            if (CollectionUtils.isEmpty(metricEntityList)) {
                return results;
            }

            for (MetricEntity newEntity : metricEntityList) {
                String resource = newEntity.getResource();
                if (resourceCount.containsKey(resource)) {
                    MetricEntity oldEntity = resourceCount.get(resource);
                    oldEntity.addPassQps(newEntity.getPassQps());
                    oldEntity.addRtAndSuccessQps(newEntity.getRt(), newEntity.getSuccessQps());
                    oldEntity.addBlockQps(newEntity.getBlockQps());
                    oldEntity.addExceptionQps(newEntity.getExceptionQps());
                    oldEntity.addCount(1);
                } else {
                    resourceCount.put(resource, MetricEntity.copyOf(newEntity));
                }
            }
            // Order by last minute b_qps DESC.
            return resourceCount.entrySet()
                    .stream()
                    .sorted((o1, o2) -> {
                        MetricEntity e1 = o1.getValue();
                        MetricEntity e2 = o2.getValue();
                        int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                        if (t != 0) {
                            return t;
                        }
                        return e2.getPassQps().compareTo(e1.getPassQps());
                    })
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private Metric toPo(MetricEntity metricEntity) {
        Metric metric = new Metric();
        BeanUtils.copyProperties(metricEntity, metric, Metric.class);
        return metric;
    }

    private MetricEntity toPo(Metric metric) {
        MetricEntity metricEntity = new MetricEntity();
        BeanUtils.copyProperties(metric, metricEntity, MetricEntity.class);
        return metricEntity;
    }
}
