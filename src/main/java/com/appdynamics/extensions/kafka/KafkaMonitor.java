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
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.YmlUtils;
import com.google.common.primitives.Booleans;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.appdynamics.extensions.kafka.utils.Constants.DEFAULT_METRIC_PREFIX;

public class KafkaMonitor extends ABaseMonitor {

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {

        List<Map<String, ?>> servers = (List<Map<String, ?>>) this.getContextConfiguration()
                .getConfigYml().get(Constants.SERVERS);
        Map<String, ?> connectionMap = (Map<String, ?>) this.getContextConfiguration()
                .getConfigYml().get(Constants.CONNECTION);
        boolean flag = false;

            for (Map server : servers) {
                if(!flag) {
                if (YmlUtils.getBoolean((server.get("useSsl")))) {
                    System.setProperty("javax.net.ssl.trustStore",
                            connectionMap.get("sslTrustStorePath").toString());
                    System.setProperty("javax.net.ssl.trustStorePassword",
                            connectionMap.get("sslTrustStorePassword").toString());
                    flag = true;
                    break;
                }
            }
        }
    }

    protected String getDefaultMetricPrefix() { return DEFAULT_METRIC_PREFIX; }

    public String getMonitorName() {
        return "Kafka Monitor";
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        List<Map<String, String>> kafkaServers = (List<Map<String, String>>)
                this.getContextConfiguration().getConfigYml().get(Constants.SERVERS);
        for (Map<String, String> kafkaServer : kafkaServers) {
            KafkaMonitorTask task = new KafkaMonitorTask(tasksExecutionServiceProvider,
                                                this.getContextConfiguration(), kafkaServer);
            //todo: do we need an assert for displayname
            AssertUtils.assertNotNull(kafkaServer.get(Constants.DISPLAY_NAME),
                    "The displayName can not be null");
            tasksExecutionServiceProvider.submit(kafkaServer.get(Constants.DISPLAY_NAME), task);
        }
    }

    protected int getTaskCount() {
        List<Map<String, String>> servers = (List<Map<String, String>>) getContextConfiguration().
                getConfigYml().get(Constants.SERVERS);
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers.size();
    }



}