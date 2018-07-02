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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.LoggerFactory;
import javax.management.remote.JMXConnector;
import java.io.IOException;
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
    private String metricPrefix;
    private String displayName;
    private JMXConnectionAdapter jmxAdapter;
    private static final BigDecimal ERROR_VALUE = BigDecimal.ZERO;
    private static final BigDecimal SUCCESS_VALUE = BigDecimal.ONE;
    private Phaser phaser;

    public KafkaMonitorTask(TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration, Map kafkaServer) {
        this.configuration = configuration;
        this.kafkaServer = kafkaServer;
        this.metricPrefix = configuration.getMetricPrefix() + "|" + kafkaServer.get("displayName");
        this.metricWriteHelper = serviceProvider.getMetricWriteHelper();
        this.displayName = (String) kafkaServer.get("displayName");
    }

    public void onTaskComplete() {

    }

    public void run() {
        phaser = new Phaser();
        populateAndPrintMetrics();

        phaser.arriveAndAwaitAdvance();
        logger.info("Completed the Kafka  Monitoring task");
    }

    private BigDecimal populateAndPrintMetrics() {
        JMXConnector jmxConnection = null;
        try{
            jmxAdapter = JMXConnectionAdapter.create(buildRequestMap());
        }catch (Exception e) {
            logger.debug("Error in connecting to Kafka" + e);
            }
        try{
            jmxConnection = jmxAdapter.open();
            logger.debug("JMX Connection is open");
            List<Map<String, ?>> mbeansFromConfig = (List<Map<String, ?>>) configuration.getConfigYml().get("mbeans");
            for (Map mbeanFromConfig : mbeansFromConfig) {

                DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor( jmxAdapter, jmxConnection, mbeanFromConfig, displayName,metricWriteHelper, metricPrefix, phaser);
                configuration.getContext().getExecutorService().execute("DomainMetricsProcessor",domainMetricsProcessor);
                logger.debug("Registering phaser for " + displayName);
            }

        } catch (Exception e) {
            logger.error("Error while opening JMX connection {}{}" + kafkaServer.get("name"), e);
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


    private Map<String, String> buildRequestMap() {
        Map<String, String> requestMap = new HashMap<String, String>();
        requestMap.put("host", kafkaServer.get("host"));
        requestMap.put("port", kafkaServer.get("port"));
        requestMap.put("username", kafkaServer.get("username"));
        requestMap.put("displayName", kafkaServer.get("displayName"));
        requestMap.put("password", getPassword(kafkaServer));
        return requestMap;
    }

    private String getPassword(Map<String, String> server) {
        String password = server.get("password");
        String encryptedPassword = server.get("encryptedPassword");
        Map<String, ?> configMap = configuration.getConfigYml();
        String encryptionKey = configMap.get("encryptionKey").toString();
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






}


