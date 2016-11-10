package com.appdynamics.extensions.kafka.metrics;


import static com.appdynamics.extensions.kafka.Util.split;

import java.util.Map;


public class MetricProperties {
    static final double DEFAULT_MULTIPLIER = 1d;
    private String alias;
    private String metricName;
    private String aggregationType;
    private String timeRollupType;
    private String clusterRollupType;
    private double multiplier = DEFAULT_MULTIPLIER;
    private boolean aggregation;
    private boolean delta;
    private Map<Object, Object> conversionValues;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getTimeRollupType() {
        return timeRollupType;
    }

    public void setTimeRollupType(String timeRollupType) {
        this.timeRollupType = timeRollupType;
    }

    public String getClusterRollupType() {
        return clusterRollupType;
    }

    public void setClusterRollupType(String clusterRollupType) {
        this.clusterRollupType = clusterRollupType;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public boolean isAggregation() {
        return aggregation;
    }

    public void setAggregation(boolean aggregation) {
        this.aggregation = aggregation;
    }

    public Map<Object, Object> getConversionValues() {
        return conversionValues;
    }

    public void setConversionValues(Map<Object, Object> conversionValues) {
        this.conversionValues = conversionValues;
    }

    public boolean isDelta() {
        return delta;
    }

    public void setDelta(boolean delta) {
        this.delta = delta;
    }

    public void setAggregationFields(String metricType) {
        String[] metricTypes = split(metricType, " ");
        this.setAggregationType(metricTypes[0]);
        this.setTimeRollupType(metricTypes[1]);
        this.setClusterRollupType(metricTypes[2]);
    }
}