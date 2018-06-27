/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka;

import static com.appdynamics.extensions.kafka.utils.Constants.DISPLAY_NAME;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.kafka.metrics.DomainMetricsProcessor;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricProperties;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
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
    private MetricWriteHelper metricWriter;
    private String metricPrefix;
    private String displayName;
    private JMXConnectionAdapter jmxAdapter;
    private static final BigDecimal ERROR_VALUE = BigDecimal.ZERO;
    private static final BigDecimal SUCCESS_VALUE = BigDecimal.ONE;

    public KafkaMonitorTask(TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration, Map kafkaServer) {
        this.configuration = configuration;
        this.kafkaServer = kafkaServer;
        this.metricPrefix = configuration.getMetricPrefix() + "|" + kafkaServer.get("displayName");
        this.metricWriter = serviceProvider.getMetricWriteHelper();
        this.displayName = (String) kafkaServer.get("displayName");
    }

    public void onTaskComplete() {

    }

    public void run() {
        populateAndPrintMetrics();
    }

    private BigDecimal populateAndPrintMetrics() {

        JMXConnector jmxConnection = null;
        try{
            jmxAdapter = JMXConnectionAdapter.create(buildRequestMap());
        } catch (Exception e) {
            logger.debug("Error in connecting to Kafka" + e);
        }

        try{
            jmxConnection = jmxAdapter.open();
            logger.debug("JMX Connection is open");
            MetricProperties metricProperties;
            List<Map<String, String>> mbeansFromConfig = (List<Map<String, String>>) configuration.getConfigYml().get("mbeans");

            for (Map mbeanFromConfig : mbeansFromConfig) {
                List<String> mbeanNames = (List<String>) mbeanFromConfig.get("objectName");

                for (String mbeanName : mbeanNames) {
                    logger.debug(String.format("Processing mbean %s from the config file", mbeanName));
                    try {

                        DomainMetricsProcessor nodeProcessor = new DomainMetricsProcessor(jmxAdapter, jmxConnection);
                        List<Metric> nodeMetrics = nodeProcessor.getNodeMetrics(mbeanName, mbeanFromConfig);
                        if (nodeMetrics.size() > 0) {
                            metricWriter.transformAndPrintMetrics(nodeMetrics);//review: if needs to be printed in every iteration
                        }


                    } catch (MalformedObjectNameException e) {
                        logger.error("Illegal object name {}" + mbeanName, e);
                    } catch (Exception e) {
                        logger.error(String.format("Error fetching JMX metrics for {%s} and mbean={%s}", kafkaServer.get("name"), mbeanName), e);
                    }
                }
            }
        } catch (IOException ioe) {
            logger.error("Error while opening JMX connection {}" + kafkaServer.get("name"));
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


