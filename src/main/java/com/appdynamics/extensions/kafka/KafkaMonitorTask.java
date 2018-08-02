/**
 * Copyright 2018 AppDynamics, Inc.
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
             logger.error("Exception occurred while collecting metrics for: {} {}",
                     this.kafkaServer.get(Constants.DISPLAY_NAME));
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
            if(configuration.getConfigYml().containsKey("encryptionKey") && !Strings.isNullOrEmpty( configuration.getConfigYml().get("encryptionKey").toString()) ) {
                jmxConnection = jmxAdapter.open(YmlUtils.getBoolean(this.kafkaServer.get("useSsl")), configuration.getConfigYml().get("encryptionKey").toString(), connectionMap);
            }

            else {
                jmxConnection = jmxAdapter.open(YmlUtils.getBoolean(this.kafkaServer.get("useSsl")), connectionMap);
            }

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
        requestMap.put(Constants.HOST, this.kafkaServer.get(Constants.HOST));
        requestMap.put(Constants.PORT, this.kafkaServer.get(Constants.PORT));
        requestMap.put(Constants.DISPLAY_NAME, this.kafkaServer.get(Constants.DISPLAY_NAME));
        requestMap.put(Constants.SERVICE_URL, this.kafkaServer.get(Constants.SERVICE_URL));
        requestMap.put(Constants.USERNAME, this.kafkaServer.get(Constants.USERNAME));
        requestMap.put(Constants.PASSWORD, getPassword());
        return requestMap;
    }

    //todo: refactor method to remove redundancy
    private String getPassword() {
            String password = this.kafkaServer.get(Constants.PASSWORD);
            if (!Strings.isNullOrEmpty(password)) { return password; }

            if(configuration.getConfigYml().containsKey(Constants.ENCRYPTION_KEY) &&
                    configuration.getConfigYml().containsKey(Constants.ENCRYPTED_PASSWORD)) {
                String encryptionKey = configuration.getConfigYml().get(Constants.ENCRYPTION_KEY).toString();
                String encryptedPassword = this.kafkaServer.get(Constants.ENCRYPTED_PASSWORD);
                if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
                    java.util.Map<String, String> cryptoMap = Maps.newHashMap();
                    cryptoMap.put(TaskInputArgs.ENCRYPTED_PASSWORD, encryptedPassword);
                    cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
                    return CryptoUtil.getPassword(cryptoMap);
                }
            }
            return null;
    }

    private Map<String,String> getConnectionParameters(){
        return (Map<String, String>) configuration.getConfigYml().get(Constants.CONNECTION);
    }

}


