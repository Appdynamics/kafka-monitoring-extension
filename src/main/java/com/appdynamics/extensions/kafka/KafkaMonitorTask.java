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
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.kafka.metrics.DomainMetricsProcessor;

import com.appdynamics.extensions.kafka.utils.Constants;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.LoggerFactory;
import javax.management.remote.JMXConnector;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

public class KafkaMonitorTask implements AMonitorTaskRunnable {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(KafkaMonitorTask.class);
    private MonitorContextConfiguration configuration;
    private Map<String, String> kafkaServer;
    private MetricWriteHelper metricWriteHelper;
    private String displayName;
    private JMXConnector jmxConnection;
    private JMXConnectionAdapter jmxAdapter;
    private static final BigDecimal ERROR_VALUE = BigDecimal.ZERO;
    private static final BigDecimal SUCCESS_VALUE = BigDecimal.ONE;
    private Phaser phaser;

    public KafkaMonitorTask(TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration, Map kafkaServer) {
        this.configuration = configuration;
        this.kafkaServer = kafkaServer;
        this.metricWriteHelper = serviceProvider.getMetricWriteHelper();
        this.displayName = (String) kafkaServer.get(Constants.DISPLAY_NAME);
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished", this.kafkaServer.get(Constants.DISPLAY_NAME));
    }

    public void run() {
        populateAndPrintMetrics();
        logger.info("Completed the Kafka  Monitoring task");
    }

    private BigDecimal populateAndPrintMetrics() {
        try{
            phaser = new Phaser();
            Map<String, String> requestMap;
            Map<String, String> connectionMap;

            requestMap = buildRequestMap();
            jmxAdapter = JMXConnectionAdapter.create(requestMap);
            connectionMap = getConnectionParameters();
            Object flag1 = connectionMap.get("useDefaultSslConnectionFactory");
            boolean flag = Boolean.valueOf(flag1.toString());
            jmxConnection = jmxAdapter.open(flag);
            logger.debug("JMX Connection is open");

            List<Map<String, ?>> mbeansFromConfig = (List<Map<String, ?>>) configuration.getConfigYml().get(Constants.MBEANS);
            for (Map mbeanFromConfig : mbeansFromConfig) {
                DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor( configuration, jmxAdapter, jmxConnection, mbeanFromConfig, displayName,metricWriteHelper, phaser);
                domainMetricsProcessor.populateMetricsForMBean();
                logger.debug("Registering phaser for " + displayName);
            }
        } catch (Exception e) {
            logger.error("Error while opening JMX connection {}{}" ,this.kafkaServer.get(Constants.DISPLAY_NAME), e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                jmxAdapter.close(jmxConnection);
                logger.debug("JMX connection is closed");
            } catch (Exception ioe) {
                logger.error("Unable to close the connection.");
                return ERROR_VALUE;
            }
        }
        return SUCCESS_VALUE;
    }

    private Map<String, String> buildRequestMap() {
        Map<String, String> requestMap = new HashMap<String, String>();
        requestMap.put("host", kafkaServer.get(Constants.HOST));
        requestMap.put("port", kafkaServer.get(Constants.PORT));
        requestMap.put("displayName", kafkaServer.get(Constants.DISPLAY_NAME));
        if(!Strings.isNullOrEmpty(kafkaServer.get(Constants.USERNAME))) {
            requestMap.put("username", kafkaServer.get(Constants.USERNAME));
            requestMap.put("password", getPassword(kafkaServer));
        }

        return requestMap;
    }


    private String getPassword(Map<String, String> server) {
        String password = server.get(Constants.PASSWORD);
        if(Strings.isNullOrEmpty(password)){
            logger.error("Password cannot be null");
        }
        String encryptedPassword = server.get(Constants.ENCRYPTED_PASSWORD);
        Map<String, ?> configMap = configuration.getConfigYml();
        String encryptionKey = configMap.get(Constants.ENCRYPTION_KEY).toString();
        if(!Strings.isNullOrEmpty(password)){
            return password;
        }
        if(!Strings.isNullOrEmpty(encryptedPassword) && !Strings.isNullOrEmpty(encryptionKey)){
            Map<String,String> cryptoMap = Maps.newHashMap();
            cryptoMap.put("password-encrypted", encryptedPassword);
            cryptoMap.put("encryption-key", encryptionKey);
            logger.debug("Decrypting the ecncrypted password........");
            return CryptoUtil.getPassword(cryptoMap);
        }
        return "";
    }

    private Map<String,String> getConnectionParameters(){
        Map<String, String> connectionMap = new HashMap<>();
        connectionMap = (Map<String, String>) configuration.getConfigYml().get("connection");
        return  connectionMap;
    }


}


