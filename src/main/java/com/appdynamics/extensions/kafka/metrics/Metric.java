/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka.metrics;

import com.google.common.base.Strings;

import java.math.BigDecimal;

public class Metric {
    private String metricName;
    private String metricKey;
    private BigDecimal metricValue;
    private MetricProperties properties;

    public String getMetricNameOrAlias() {
        if(properties == null || Strings.isNullOrEmpty(properties.getAlias())){
            return metricName;
        }
        return properties.getAlias();
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public MetricProperties getProperties() {
        return properties;
    }

    public void setProperties(MetricProperties properties) {
        this.properties = properties;
    }

}
