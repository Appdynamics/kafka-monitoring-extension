/**
 * Copyright 2014 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.monitors.kafka;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.util.metrics.Metric;
import com.appdynamics.extensions.util.metrics.MetricFactory;
import com.appdynamics.extensions.yml.YmlReader;
import com.appdynamics.monitors.kafka.config.Configuration;
import com.appdynamics.monitors.kafka.config.Domain;
import com.appdynamics.monitors.kafka.config.KafkaMonitorConstants;
import com.appdynamics.monitors.kafka.config.Server;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KafkaMonitor extends AManagedMonitor {
    private static final Logger logger = Logger.getLogger(KafkaMonitor.class);

    public static final String METRIC_SEPARATOR = "|";
    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/KafkaMonitor/config.yml";


    public KafkaMonitor() {
        String details = KafkaMonitor.class.getPackage().getImplementationTitle();
        String msg = "Using Monitor Version [" + details + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info("Starting the Kafka Monitoring task.");
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);
                Map<String, Number> metrics = populateStats(config);
                //metric overrides
                MetricFactory<Number> metricFactory = new MetricFactory<Number>(config.getMetricOverrides());
                List<Metric> allMetrics = metricFactory.process(metrics);
                printStats(config, allMetrics);
                logger.info("Completed the Kafka Monitoring Task successfully");
                return new TaskOutput("Kafka Monitor executed successfully");
            } catch (FileNotFoundException e) {
                logger.error("Config File not found: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("Kafka Monitor completed with failures");
    }

    private Map<String, Number> populateStats(Configuration config) throws Exception {
        Map<String, Number> metrics = new HashMap<String, Number>();
        Server server = config.getServer();
        JMXConnector connector = null;
        try {
            connector = KafkaJMXConnector.connect(server);
            List<Domain> domains = server.getDomains();
            for (Domain domain : domains) {
                Set<ObjectInstance> mBeans = KafkaJMXConnector.queryMBeans(connector, domain.getName());
                if (mBeans != null) {
                    Map<String, Number> curMetrics = extractMetrics(connector, domain, mBeans);
                    metrics.putAll(curMetrics);
                } else {
                    logger.debug("Error while getting data from MBean domain" + domain);
                }
            }
            metrics.put(KafkaMonitorConstants.METRICS_COLLECTED, KafkaMonitorConstants.SUCCESS_VALUE);
        } catch (Exception e) {
            logger.error("Error JMX-ing into Kafka Server ", e);
            metrics.put(KafkaMonitorConstants.METRICS_COLLECTED, KafkaMonitorConstants.ERROR_VALUE);
        } finally {
            KafkaJMXConnector.close(connector);
        }
        return metrics;
    }

    private Map<String, Number> extractMetrics(JMXConnector connector, Domain domain, Set<ObjectInstance> allMbeans) {
        Map<String, Number> metrics = new HashMap<String, Number>();
        List<String> excludeObjects = domain.getExcludeObjects();
        for (ObjectInstance mbean : allMbeans) {
            ObjectName objectName = mbean.getObjectName();

            String name = objectName.getKeyProperty("name");
            if (excludeObjects.contains(name)) {
                String type = objectName.getKeyProperty("type");
                logger.debug("Excluding [" + name + "] of type [" + type + "] in domain [" + domain.getName() + "] as configured");
                continue; // Skip if the attribute is excluded
            }
            MBeanAttributeInfo[] attributes = KafkaJMXConnector.fetchAllAttributesForMbean(connector, objectName);
            if (attributes != null) {
                for (MBeanAttributeInfo attr : attributes) {
                    if (attr.isReadable()) {
                        Object attribute = KafkaJMXConnector.getMBeanAttribute(connector, objectName, attr.getName());
                        Number val = getNumberValue(attribute);
                        if (val != null) {
                            String metricKey = getMetricsKey(objectName, attr);
                            metrics.put(metricKey, val);
                        } else {
                            logger.debug("Ignoring " + attr.getName() + " of " + objectName.getKeyProperty("type") + " as its value is null or not a number");
                        }
                    }
                }
            }
        }
        return metrics;
    }

    private Number getNumberValue(Object attribute) {

        try {
            Double val = Double.parseDouble(String.valueOf(attribute));
            return val;
        } catch (Exception e) {
        }
        return null;
    }


    private String getMetricsKey(ObjectName objectName, MBeanAttributeInfo attr) {
        StringBuilder metricsKey = new StringBuilder();

        String type = objectName.getKeyProperty("type");
        String name = objectName.getKeyProperty("name");

        metricsKey.append(objectName.getDomain()).append(METRIC_SEPARATOR).append(type).append(METRIC_SEPARATOR).append(name);
        metricsKey.append(METRIC_SEPARATOR).append(attr.getName());
        return metricsKey.toString();
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    private void printStats(Configuration config, List<Metric> metrics) {
        String metricPathPrefix = config.getMetricPathPrefix();
        for (Metric aMetric : metrics) {
            printMetric(metricPathPrefix + aMetric.getMetricPath(), aMetric.getMetricValue().toString(), aMetric.getAggregator(), aMetric.getTimeRollup(), aMetric.getClusterRollup());
        }
    }


    private void printMetric(String metricName, String metricValue, String aggType, String timeRollupType, String clusterRollupType) {
        MetricWriter metricWriter = getMetricWriter(metricName,
                aggType,
                timeRollupType,
                clusterRollupType
        );
        //System.out.println("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
        //        + "] metric = " + metricName + " = " + metricValue);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
                    + "] metric = " + metricName + " = " + metricValue);
        }
        metricWriter.printMetric(metricValue);
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);
        logger.getRootLogger().addAppender(ca);

        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/Users/Muddam/AppDynamics/Code/extensions/kafka-monitoring-extension/src/main/resources/config/config.yml");

        KafkaMonitor kafkaMonitor = new KafkaMonitor();
        kafkaMonitor.execute(taskArgs, null);
    }
}
