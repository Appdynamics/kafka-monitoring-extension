/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TaskInputArgs;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.kafka.metrics.DomainMetricsProcessor;
import com.appdynamics.extensions.kafka.utils.Constants;
import com.appdynamics.extensions.util.YmlUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
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
    private Map<String, String> kafkaServer;
    private MetricWriteHelper metricWriteHelper;
    private String displayName;
    private JMXConnector jmxConnection;
    private JMXConnectionAdapter jmxAdapter;

    KafkaMonitorTask(TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration,
                     Map kafkaServer) {
        this.configuration = configuration;
        this.kafkaServer = kafkaServer;
        this.metricWriteHelper = serviceProvider.getMetricWriteHelper();
        this.displayName = (String) kafkaServer.get(Constants.DISPLAY_NAME);
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished", this.kafkaServer.get(Constants.DISPLAY_NAME));
    }

    public void run() {
         try {
             populateAndPrintMetrics();
             logger.info("Completed Kafka Monitoring task for Kafka server: {}",
                     this.kafkaServer.get(Constants.DISPLAY_NAME));
         }catch(Exception e ) {
             logger.error("Exception occurred while collecting metrics for: {} {}", this.kafkaServer.get(Constants.DISPLAY_NAME));
         }
    }

    public void populateAndPrintMetrics() {
        try{
            BigDecimal connectionStatus = openJMXConnection();
            List<Map<String, ?>> mbeansFromConfig = (List<Map<String, ?>>) configuration.getConfigYml()
                    .get(Constants.MBEANS);
            for (Map mbeanFromConfig : mbeansFromConfig) {
                DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor(configuration, jmxAdapter,
                        jmxConnection, mbeanFromConfig, displayName, metricWriteHelper, connectionStatus);
                domainMetricsProcessor.populateMetricsForMBean();
            }
        } catch (Exception e) {
            logger.error("Error while opening JMX connection: {}  {}" ,this.kafkaServer.get(Constants.DISPLAY_NAME), e);
        } finally {
            try {
                jmxAdapter.close(jmxConnection);
                logger.debug("JMX connection is closed");
            } catch (Exception ioe) {
                logger.error("Unable to close the connection: {} ", ioe);
            }
        }
    }

    private BigDecimal openJMXConnection() {
        try {
            Map<String, String> requestMap = buildRequestMap();
            jmxAdapter = JMXConnectionAdapter.create(requestMap);
            Map<String, String> connectionMap = getConnectionParameters();
            jmxConnection = jmxAdapter.open(YmlUtils.getBoolean(connectionMap.get("useDefaultSslConnectionFactory")),
                    YmlUtils.getBoolean(this.kafkaServer.get("useSsl")));
            logger.debug("JMX Connection is open to Kafka server: {}", this.kafkaServer.get(Constants.DISPLAY_NAME));
            return BigDecimal.ONE;
        } catch (IOException ioe) {
            logger.error("Unable to open a JMX Connection Kafka server: {} {} "
                    , this.kafkaServer.get(Constants.DISPLAY_NAME), ioe);
        }
        return BigDecimal.ZERO;
    }

    private Map<String, String> buildRequestMap() {
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(Constants.HOST, this. kafkaServer.get(Constants.HOST));
        requestMap.put(Constants.PORT, this.kafkaServer.get(Constants.PORT));
        requestMap.put(Constants.DISPLAY_NAME, this.kafkaServer.get(Constants.DISPLAY_NAME));
        requestMap.put(Constants.SERVICE_URL, this.kafkaServer.get(Constants.SERVICE_URL));
        requestMap.put(Constants.USERNAME, this.kafkaServer.get(Constants.USERNAME));
        requestMap.put(Constants.PASSWORD, getPassword());
        return requestMap;
    }

    private String getPassword() {
            String password = this.kafkaServer.get(Constants.PASSWORD);
            if (!Strings.isNullOrEmpty(password)) { return password; }
            String encryptionKey = configuration.getConfigYml().get(Constants.ENCRYPTION_KEY).toString();
            String encryptedPassword = this.kafkaServer.get(Constants.ENCRYPTED_PASSWORD);
            if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
                java.util.Map<String, String> cryptoMap = Maps.newHashMap();
                cryptoMap.put(TaskInputArgs.ENCRYPTED_PASSWORD, encryptedPassword);
                cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
                return CryptoUtil.getPassword(cryptoMap);
            }
            return null;
    }

    private Map<String,String> getConnectionParameters(){
        Map<String, String> connectionMap = (Map<String, String>) configuration.getConfigYml().get(Constants.CONNECTION);
        return  connectionMap;
    }

}


