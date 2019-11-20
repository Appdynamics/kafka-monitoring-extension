/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.kafka.utils.Constants;
import com.appdynamics.extensions.kafka.utils.SslUtils;
import com.appdynamics.extensions.util.AssertUtils;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.kafka.utils.Constants.DEFAULT_METRIC_PREFIX;

public class KafkaMonitor extends ABaseMonitor {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(KafkaMonitor.class);

    @Override
    protected void onConfigReload (File file) {
        Map<String, ?> configMap = this.getContextConfiguration().getConfigYml();
        //#TODO We need to find a better way to handle SSL in JMX.
        SslUtils sslUtils = new SslUtils();
        sslUtils.setSslProperties(configMap);
    }

    protected String getDefaultMetricPrefix () {
        return DEFAULT_METRIC_PREFIX;
    }

    public String getMonitorName () {
        return Constants.KAFKA_MONITOR;
    }

    protected void doRun (TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        List<Map<String, String>> kafkaServers = (List<Map<String, String>>)
                this.getContextConfiguration().getConfigYml().get(Constants.SERVERS);
        logger.info("The size of servers section is: "+kafkaServers);

        Map<String, ?> kubernetesConfig = (Map<String, ?>) this.getContextConfiguration().getConfigYml().get("kubernetes");

        Boolean kubernetesMode = Boolean.valueOf(kubernetesConfig.get("useKubernetes").toString());

        for (Map<String, String> kafkaServer : kafkaServers) {

            AssertUtils.assertNotNull(kafkaServer, "the server arguments are empty");
            if (kafkaServer.size() > 1 && !kubernetesMode) {
                AssertUtils.assertNotNull(kafkaServer.get("displayName"), "The displayName can not be null");
                logger.info("Starting the Kafka Monitoring Task for server : " + kafkaServer.get("displayName"));
            } else {
                logger.info("Starting the Nginx Monitoring Task");
            }

            KafkaMonitorTask task = new KafkaMonitorTask(tasksExecutionServiceProvider,
                    this.getContextConfiguration(), kafkaServer);
            tasksExecutionServiceProvider.submit(kafkaServer.get(Constants.DISPLAY_NAME), task);
        }
    }

    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) getContextConfiguration().
                getConfigYml().get(Constants.SERVERS);
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialized");
        return servers;
    }


    public static void main(String[] args) throws TaskExecutionException {


        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);
        org.apache.log4j.Logger.getRootLogger().addAppender(ca);

        KafkaMonitor monitor = new KafkaMonitor();

        final Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put("config-file", "/Users/satishrm/AppDynamics/Code/extensions/kafka-monitoring-extension/src/main/resources/conf/config.yml");

        monitor.execute(taskArgs, null);

    }

}