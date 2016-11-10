package com.appdynamics.extensions.kafka.metrics;


import static com.appdynamics.extensions.kafka.Util.toBigIntString;

import com.appdynamics.extensions.util.MetricWriteHelper;
import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class MetricPrinter {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MetricPrinter.class);

    private int totalMetricsReported;
    private String metricPrefix;
    private String displayName;
    private MetricWriteHelper metricWriter;

    public MetricPrinter(String metricPrefix, String displayName, MetricWriteHelper metricWriter) {
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.metricWriter = metricWriter;
    }

    public void reportNodeMetrics(final List<Metric> componentMetrics) {
        if (componentMetrics == null || componentMetrics.isEmpty()) {
            return;
        }
        for (Metric metric : componentMetrics) {
            MetricProperties props = metric.getProperties();
            String fullMetricPath = formMetricPath(metric.getMetricKey());
            printMetric(fullMetricPath, metric.getMetricValue(), props.getAggregationType(), props.getTimeRollupType(), props.getClusterRollupType());
        }
    }

    public void printMetric(String metricPath, BigDecimal metricValue, String aggType, String timeRollupType, String clusterRollupType) {
        try {
            String metricValStr = toBigIntString(metricValue);
            if (metricValStr != null) {
                metricWriter.printMetric(metricPath, metricValStr, aggType, timeRollupType, clusterRollupType);
                logger.debug("Sending [{}|{}|{}] metric= {},value={}", aggType, timeRollupType, clusterRollupType, metricPath, metricValStr);
                totalMetricsReported++;
            }
        } catch (Exception e) {
            logger.error("Error reporting metric {} with value {}", metricPath, metricValue, e);
        }
    }

    public String formMetricPath(String metricKey) {
        if (!Strings.isNullOrEmpty(displayName)) {
            return metricPrefix + "|" + displayName + "|" + metricKey;
        }
        return metricPrefix + "|" + metricKey;
    }

    public int getTotalMetricsReported() {
        return totalMetricsReported;
    }
}
