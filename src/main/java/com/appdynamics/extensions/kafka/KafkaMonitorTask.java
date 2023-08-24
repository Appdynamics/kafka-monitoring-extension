/**
 * Copyright 2018 AppDynamics, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.kafka.metrics.DomainMetricsProcessor;
import com.appdynamics.extensions.kafka.utils.Constants;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaMonitorTask implements AMonitorTaskRunnable {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(KafkaMonitorTask.class);
    private MonitorContextConfiguration configuration;
    private Map<String, ?> kafkaServer;
    private MetricWriteHelper metricWriteHelper;
    private String displayName;
    private JMXConnectionAdapter jmxAdapter;
    private JMXConnector jmxConnector;

    KafkaMonitorTask (TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration,
                      Map<String, String> kafkaServer) {
        this.configuration = configuration;
        this.kafkaServer = kafkaServer;
        this.metricWriteHelper = serviceProvider.getMetricWriteHelper();
        this.displayName = kafkaServer.get(Constants.DISPLAY_NAME);
    }

    public void onTaskComplete () {
        logger.info("All tasks for server {} finished", this.kafkaServer.get(Constants.DISPLAY_NAME));
    }

    public void run () {
        try {
            logger.info("Starting Kafka Monitoring task for Kafka server: {} ", this.kafkaServer.get(Constants.DISPLAY_NAME));
            populateAndPrintMetrics();
            populateAndPrintLagMetrics();
            logger.info("Completed Kafka Monitoring task for Kafka server: {}",
                    this.kafkaServer.get(Constants.DISPLAY_NAME));
        } catch (Exception e) {
            logger.error("Exception occurred while collecting metrics for: {} {}",
                    this.kafkaServer.get(Constants.DISPLAY_NAME), e);
        }
    }

    public void populateAndPrintMetrics () {
        try {
            BigDecimal connectionStatus = openJMXConnection();
            metricWriteHelper.printMetric(this.configuration.getMetricPrefix() +
                            Constants.METRIC_SEPARATOR + this.displayName + Constants.METRIC_SEPARATOR + "kafka.server" +
                            Constants.METRIC_SEPARATOR + "HeartBeat", connectionStatus.toString(), Constants.AVERAGE, Constants.AVERAGE, Constants.INDIVIDUAL);
            if(connectionStatus.equals(BigDecimal.ONE)) {
                List<Map<String, ?>> mBeansListFromConfig = (List<Map<String, ?>>) configuration.getConfigYml()
                        .get(Constants.MBEANS);
                DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor(configuration, jmxAdapter,
                        jmxConnector, displayName, metricWriteHelper);
                for (Map mbeanFromConfig : mBeansListFromConfig) {
                    domainMetricsProcessor.populateMetricsForMBean(mbeanFromConfig);
                }
            }

        } catch (Exception e) {
            logger.error("Error while opening JMX connection: {}  {}", this.kafkaServer.get(Constants.DISPLAY_NAME), e);
        } finally {
            try {
                jmxAdapter.close(jmxConnector);
                logger.debug("JMX connection is closed");
            } catch (Exception ioe) {
                logger.error("Unable to close the connection: {} ", ioe);
            }
        }
    }
    public void populateAndPrintLagMetrics () {
        try {
            List<Map> groups = (List<Map>) this.kafkaServer.get(Constants.CONSUMERGROUPS);
            logger.info("HERE2");
            String host = (String) this.kafkaServer.get(Constants.HOST) + ':' + (String) this.kafkaServer.get(Constants.CONSUMER_PORT);
            ConsumerGroupLag cgl = new ConsumerGroupLag();
            logger.info("HERE3");
            for (Map<String, ?> group : groups) {
                logger.info("HERE4");
                String groupid = (String) group.get(Constants.GROUPID);
                String topic = (String) group.get(Constants.TOPIC);
                Map<TopicPartition, ConsumerGroupLag.PartionOffsets> lag = cgl.getConsumerGroupOffsets(host, topic, groupid);
                logger.info(lag.toString());

                for (Map.Entry<TopicPartition, ConsumerGroupLag.PartionOffsets> entry : lag.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    ConsumerGroupLag.PartionOffsets offsets = entry.getValue();
                    logger.info("Key = " + entry.getKey() + ", Value = " + offsets.toString());

                    metricWriteHelper.printMetric(this.configuration.getMetricPrefix() +
                                    Constants.METRIC_SEPARATOR + "ConsumerLag" + Constants.METRIC_SEPARATOR + offsets.getTopic() + '-' + groupid + Constants.METRIC_SEPARATOR + offsets.getPartition() +
                                    Constants.METRIC_SEPARATOR + "currentOffset", String.valueOf(offsets.getCurrentOffset()),
                            Constants.AVERAGE, Constants.AVERAGE, Constants.INDIVIDUAL);

                    metricWriteHelper.printMetric(this.configuration.getMetricPrefix() +
                                    Constants.METRIC_SEPARATOR + "ConsumerLag" + Constants.METRIC_SEPARATOR + offsets.getTopic() + '-' + groupid + Constants.METRIC_SEPARATOR + offsets.getPartition() +
                                    Constants.METRIC_SEPARATOR + "endOffset",  String.valueOf(offsets.endOffset),
                            Constants.AVERAGE, Constants.AVERAGE, Constants.INDIVIDUAL);

                    metricWriteHelper.printMetric(this.configuration.getMetricPrefix() +
                                    Constants.METRIC_SEPARATOR + "ConsumerLag" + Constants.METRIC_SEPARATOR + offsets.getTopic() + '-' + groupid + Constants.METRIC_SEPARATOR + offsets.getPartition() +
                                    Constants.METRIC_SEPARATOR + "lag", String.valueOf(offsets.getLag()),
                            Constants.AVERAGE, Constants.AVERAGE, Constants.INDIVIDUAL);

                }
            }
        } catch (Exception e) {
            logger.error("Error while getting Lag metrics: {}  {}", this.kafkaServer.get(Constants.DISPLAY_NAME), e);
        }
    }

    private BigDecimal openJMXConnection () {
        try {
            Map<String, String> requestMap = buildRequestMap();
            jmxAdapter = JMXConnectionAdapter.create(requestMap);
            Map<String, Object> connectionMap = (Map<String, Object>) getConnectionParameters();
            if (configuration.getConfigYml().containsKey(Constants.ENCRYPTION_KEY) &&
                    !Strings.isNullOrEmpty(configuration.getConfigYml().get(Constants.ENCRYPTION_KEY).toString())) {
                connectionMap.put(Constants.ENCRYPTION_KEY, configuration.getConfigYml().get(Constants.ENCRYPTION_KEY).toString());
            } else {
                connectionMap.put(Constants.ENCRYPTION_KEY, "");
            }
            jmxConnector = jmxAdapter.open(connectionMap);
            if (jmxConnector != null) {
                logger.debug("JMX Connection is open to Kafka server: {}", this.kafkaServer.get(Constants.DISPLAY_NAME));
                return BigDecimal.ONE;
            }

            return BigDecimal.ZERO;
        } catch (IOException ioe) {
            logger.error("Unable to open a JMX Connection Kafka server: {} {} "
                    , this.kafkaServer.get(Constants.DISPLAY_NAME), ioe);
        }
        return null;
    }

    private Map<String, String> buildRequestMap () {
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(Constants.HOST, (String)this.kafkaServer.get(Constants.HOST));
        requestMap.put(Constants.PORT, (String)this.kafkaServer.get(Constants.PORT));
        requestMap.put(Constants.DISPLAY_NAME, (String)this.kafkaServer.get(Constants.DISPLAY_NAME));
        requestMap.put(Constants.SERVICE_URL, (String)this.kafkaServer.get(Constants.SERVICE_URL));
        requestMap.put(Constants.USERNAME, (String)this.kafkaServer.get(Constants.USERNAME));
        requestMap.put(Constants.PASSWORD, getPassword());
        return requestMap;
    }

    private String getPassword () {
        String password = (String)this.kafkaServer.get(Constants.PASSWORD);
        Map<String, ?> configMap = configuration.getConfigYml();
        java.util.Map<String, String> cryptoMap = Maps.newHashMap();
        cryptoMap.put(Constants.PASSWORD, password);
        if (configMap.containsKey(Constants.ENCRYPTION_KEY)) {
            String encryptionKey = configMap.get(Constants.ENCRYPTION_KEY).toString();
            String encryptedPassword = (String)this.kafkaServer.get(Constants.ENCRYPTED_PASSWORD);
            if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
                cryptoMap.put(Constants.ENCRYPTED_PASSWORD, encryptedPassword);
                cryptoMap.put(Constants.ENCRYPTION_KEY, encryptionKey);
            }
        }
        return CryptoUtils.getPassword(cryptoMap);
    }

    private Map<String, ?> getConnectionParameters () {
        if (configuration.getConfigYml().containsKey(Constants.CONNECTION))
            return (Map<String, ?>) configuration.getConfigYml().get(Constants.CONNECTION);
        else
            return new HashMap<String, String>();
    }

}
