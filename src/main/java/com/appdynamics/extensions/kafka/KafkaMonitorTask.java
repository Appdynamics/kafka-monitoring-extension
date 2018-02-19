/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka;

import static com.appdynamics.extensions.kafka.ConfigConstants.DISPLAY_NAME;
import static com.appdynamics.extensions.kafka.Util.convertToString;

import com.appdynamics.extensions.kafka.metrics.DomainMetricsProcessor;
import com.appdynamics.extensions.kafka.metrics.Metric;
import com.appdynamics.extensions.kafka.metrics.MetricPrinter;
import com.appdynamics.extensions.kafka.metrics.MetricProperties;
import com.appdynamics.extensions.kafka.metrics.MetricPropertiesBuilder;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Satish Muddam
 */
public class KafkaMonitorTask implements Runnable {
    private static final Logger logger = Logger.getLogger(KafkaMonitorTask.class);
    private static final String METRICS_COLLECTION_SUCCESSFUL = "Metrics Collection Successful";
    private static final BigDecimal ERROR_VALUE = BigDecimal.ZERO;
    private static final BigDecimal SUCCESS_VALUE = BigDecimal.ONE;


    private String displayName;
    /* metric prefix from the config.yaml to be applied to each metric path*/
    private String metricPrefix;

    /* server properties */
    private Map server;

    /* a facade to report metrics to the machine agent.*/
    private MetricWriteHelper metricWriter;

    /* a stateless JMX adapter that abstracts out all JMX methods.*/
    private JMXConnectionAdapter jmxAdapter;

    /* config mbeans from config.yaml. */
    private List<Map> configMBeans;

    public void run() {
        displayName = convertToString(server.get(DISPLAY_NAME), "");
        long startTime = System.currentTimeMillis();
        MetricPrinter metricPrinter = new MetricPrinter(metricPrefix, displayName, metricWriter);
        try {
            logger.debug(String.format("Kafka monitor thread for server {} started.", displayName));
            BigDecimal status = extractAndReportMetrics(metricPrinter);
            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), status
                    , MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        } catch (Exception e) {
            logger.error(String.format("Error in Kafka Monitor thread for server {}", displayName), e);
            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), ERROR_VALUE
                    , MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

        } finally {
            long endTime = System.currentTimeMillis() - startTime;
            logger.debug(String.format("Kafka monitor thread for server {%s} ended. Time taken = {%s} and Total metrics reported = {%d}", displayName, endTime, metricPrinter.getTotalMetricsReported()));
        }
    }

    private BigDecimal extractAndReportMetrics(final MetricPrinter metricPrinter) throws Exception {
        JMXConnector jmxConnection = null;
        try {
            jmxConnection = jmxAdapter.open();
            logger.debug("JMX Connection is open");
            MetricPropertiesBuilder propertyBuilder = new MetricPropertiesBuilder();
            for (Map aConfigMBean : configMBeans) {

                List<String> mbeanNames = (List<String>) aConfigMBean.get("mbeanFullPath");

                for (String mbeanFullName : mbeanNames) {

                    String configObjectName = convertToString(mbeanFullName, "");
                    logger.debug(String.format("Processing mbean %s from the config file", mbeanFullName));
                    try {
                        Map<String, MetricProperties> metricPropsMap = propertyBuilder.build(aConfigMBean);
                        DomainMetricsProcessor nodeProcessor = new DomainMetricsProcessor(jmxAdapter, jmxConnection);
                        List<Metric> nodeMetrics = nodeProcessor.getNodeMetrics(mbeanFullName, aConfigMBean, metricPropsMap);
                        if (nodeMetrics.size() > 0) {
                            metricPrinter.reportNodeMetrics(nodeMetrics);
                        }

                    } catch (MalformedObjectNameException e) {
                        logger.error("Illegal object name {}" + configObjectName, e);
                        throw e;
                    } catch (Exception e) {
                        //System.out.print("" + e);
                        logger.error(String.format("Error fetching JMX metrics for {%s} and mbean={%s}", displayName, configObjectName), e);
                        throw e;
                    }
                }
            }
        } finally {
            try {
                jmxAdapter.close(jmxConnection);
                logger.debug("JMX connection is closed");
            } catch (IOException ioe) {
                logger.error("Unable to close the connection.");
                return ERROR_VALUE;
            }
        }
        return SUCCESS_VALUE;
    }


    static class Builder {
        private KafkaMonitorTask task = new KafkaMonitorTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder server(Map server) {
            task.server = server;
            return this;
        }

        Builder jmxConnectionAdapter(JMXConnectionAdapter adapter) {
            task.jmxAdapter = adapter;
            return this;
        }

        Builder mbeans(List<Map> mBeans) {
            task.configMBeans = mBeans;
            return this;
        }

        KafkaMonitorTask build() {
            return task;
        }
    }
}

