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
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class KafkaMonitor extends ABaseMonitor {
    private static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|Kafka";
    private static final Logger logger = LoggerFactory.getLogger(KafkaMonitor.class);

    @Override
    protected void initializeMoreStuff(Map<String, String> args){

    }

    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Kafka Extension";
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        List<Map<String, String>> kafkaServers = (List<Map<String, String>>) this.getContextConfiguration().getConfigYml().get("servers");
        for (Map<String, String> kafkaServer : kafkaServers) {

            KafkaMonitorTask task = new KafkaMonitorTask(tasksExecutionServiceProvider, this.getContextConfiguration(), kafkaServer);
            AssertUtils.assertNotNull(kafkaServer.get("displayName"), "The displayName can not be null");
            tasksExecutionServiceProvider.submit((String) kafkaServer.get("displayName"), task);
        }

    }

    @Override
    protected int getTaskCount() {
        List<Map<String, String>> servers = (List<Map<String, String>>) getContextConfiguration().getConfigYml().get("servers");
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers.size();
    }

}
