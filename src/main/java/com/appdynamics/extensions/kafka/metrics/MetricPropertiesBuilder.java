/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka.metrics;


import static com.appdynamics.extensions.kafka.ConfigConstants.INCLUDE;
import static com.appdynamics.extensions.kafka.ConfigConstants.METRICS;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;


public class MetricPropertiesBuilder {

    public Map<String, MetricProperties> build(Map aConfigMBean) {
        Map<String, MetricProperties> metricPropsMap = Maps.newHashMap();
        if (aConfigMBean == null || aConfigMBean.isEmpty()) {
            return metricPropsMap;
        }
        Map configMetrics = (Map) aConfigMBean.get(METRICS);
        List includeMetrics = (List) configMetrics.get(INCLUDE);
        if (includeMetrics != null) {
            for (Object metad : includeMetrics) {
                Map localMetaData = (Map) metad;
                Map.Entry entry = (Map.Entry) localMetaData.entrySet().iterator().next();
                String metricName = entry.getKey().toString();
                String alias = entry.getValue().toString();
                MetricProperties props = new DefaultMetricProperties();
                props.setAlias(alias);
                props.setMetricName(metricName);
                setProps(aConfigMBean, props); //global level
                setProps(localMetaData, props); //local level
                metricPropsMap.put(metricName, props);
            }
        }
        return metricPropsMap;
    }

    private void setProps(Map metadata, MetricProperties props) {
        if (metadata.get("metricType") != null) {
            props.setAggregationFields(metadata.get("metricType").toString());
        }
        if (metadata.get("multiplier") != null) {
            props.setMultiplier(Double.parseDouble(metadata.get("multiplier").toString()));
        }
        if (metadata.get("convert") != null) {
            props.setConversionValues((Map) metadata.get("convert"));
        }
        if (metadata.get("aggregation") != null) {
            props.setAggregation(Boolean.parseBoolean(metadata.get("aggregation").toString()));
        }
        if (metadata.get("delta") != null) {
            props.setDelta(Boolean.parseBoolean(metadata.get("delta").toString()));
        }
    }

}
