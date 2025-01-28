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

import org.apache.kafka.clients.admin.*;
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
import java.util.*;
import java.util.regex.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
            logger.debug("Starting Kafka Monitoring task for Kafka server: {} ", this.kafkaServer.get(Constants.DISPLAY_NAME));
            populateAndPrintMetrics();
            populateAndPrintLagMetrics();
            logger.debug("Completed Kafka Monitoring task for Kafka server: {}",
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

            String topicRegex =  (String) this.kafkaServer.get(Constants.TOPIC);
            String host = (String) this.kafkaServer.get(Constants.HOST) + ':' + (String) this.kafkaServer.get(Constants.CONSUMER_PORT);

            Properties properties = new Properties();
            properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, host);
            AdminClient kafkaAdmin = AdminClient.create(properties);

            List<String> groupIds = kafkaAdmin.listConsumerGroups().all().get().stream().map(s -> s.groupId()).collect(Collectors.toList());
            Set<String> topics = kafkaAdmin.listTopics().names().get();
            ConsumerGroupLag cgl = new ConsumerGroupLag();
            Map<TopicPartition, Long> lagSet = new ConcurrentHashMap<>();

            logger.debug("AA Scanning Groups to check for lags");

            for (final String topicName : topics) {
                logger.debug("AA ReviewingTopic:" + topicName);
                boolean isMatch = Pattern.matches(topicRegex, topicName);
                if (isMatch) {
                    for (final String groupId : groupIds) {
                        logger.debug("AA ReviewingTopic:" + topicName + " GroupName:" + groupId);
                        Map<TopicPartition, ConsumerGroupLag.PartionOffsets> lag = cgl.getConsumerGroupOffsets(host, topicName, groupId);
                        for (Map.Entry<TopicPartition, ConsumerGroupLag.PartionOffsets> entry : lag.entrySet()) {
                            ConsumerGroupLag.PartionOffsets offsets = entry.getValue();
                            if (lagSet.containsKey(entry.getKey())) {
                                logger.debug("AA Updating TopicPartition:" + entry.getKey().toString() + "  CurrLag:" + lagSet.get(entry.getKey()) + "  AddingLag:" + offsets.getLag());
                                lagSet.put(entry.getKey(), (long) (lagSet.get(entry.getKey()) + offsets.getLag()));
                            } else {
                                logger.debug("AA Inserting TopicPartition:" + entry.getKey().toString() + "  CurrLag:NONE   AddingLag:" + offsets.getLag());
                                lagSet.put(entry.getKey(), (long) offsets.getLag());
                            }
                        }
                    }
                }
            }

            logger.debug("AA DONE  Scanning Groups. Writing Metrics. ");
            for (Map.Entry<TopicPartition, Long> entry : lagSet.entrySet()) {
                logger.debug("AA METRIC WRITE Topic:" + entry.getKey().topic() + ", Partition:" + entry.getKey().partition() + ", Lag:" + entry.getValue());

                metricWriteHelper.printMetric(this.configuration.getMetricPrefix() +
                        Constants.METRIC_SEPARATOR + this.displayName + Constants.METRIC_SEPARATOR + "kafka.lag" +
                        Constants.METRIC_SEPARATOR + entry.getKey().topic( ) + Constants.METRIC_SEPARATOR + entry.getKey().partition() +
                        Constants.METRIC_SEPARATOR + "lag", String.valueOf(entry.getValue()), Constants.AVERAGE, Constants.AVERAGE, Constants.INDIVIDUAL);
            }
            logger.debug("AA DONE Writing Metrics. Closing connection ");
            kafkaAdmin.close();

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
