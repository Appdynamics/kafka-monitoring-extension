/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka;

import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;
import static com.appdynamics.extensions.kafka.ConfigConstants.DISPLAY_NAME;
import static com.appdynamics.extensions.kafka.ConfigConstants.ENCRYPTED_PASSWORD;
import static com.appdynamics.extensions.kafka.ConfigConstants.HOST;
import static com.appdynamics.extensions.kafka.ConfigConstants.INSTANCES;
import static com.appdynamics.extensions.kafka.ConfigConstants.MBEANS;
import static com.appdynamics.extensions.kafka.ConfigConstants.PASSWORD;
import static com.appdynamics.extensions.kafka.ConfigConstants.PORT;
import static com.appdynamics.extensions.kafka.ConfigConstants.SERVICE_URL;
import static com.appdynamics.extensions.kafka.ConfigConstants.USERNAME;
import static com.appdynamics.extensions.kafka.Util.convertToString;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KafkaMonitor extends AManagedMonitor {
    private static final Logger logger = Logger.getLogger(KafkaMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|Kafka|";


    private boolean initialized;
    private MonitorConfiguration configuration;

    public KafkaMonitor() {
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
        logVersion();
        if (!initialized) {
            initialize(taskArgs);
        }
        logger.debug(String.format("The raw arguments are {}", taskArgs));
        configuration.executeTask();
        logger.info("Kafka monitor run completed successfully.");
        return new TaskOutput("Kafka monitor run completed successfully.");

    }

    private void initialize(Map<String, String> taskArgs) {
        if (!initialized) {
            //read the config.
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                    MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            if (config != null) {
                List<Map> servers = (List<Map>) config.get(INSTANCES);
                if (servers != null && !servers.isEmpty()) {

                    for (Map server : servers) {
                        try {
                            KafkaMonitorTask task = createTask(server);
                            configuration.getExecutorService().execute(task);
                        } catch (IOException e) {
                            logger.error(String.format("Cannot construct JMX uri for {}", convertToString(server.get(DISPLAY_NAME), "")));
                        }

                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                logger.error("The config.yml is not loaded due to previous errors.The task will not run");
            }
        }
    }

    private KafkaMonitorTask createTask(Map server) throws IOException {
        String serviceUrl = convertToString(server.get(SERVICE_URL), "");
        String host = convertToString(server.get(HOST), "");
        String portStr = convertToString(server.get(PORT), "");
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        String username = convertToString(server.get(USERNAME), "");
        String password = getPassword(server);

        JMXConnectionAdapter adapter = JMXConnectionAdapter.create(serviceUrl, host, port, username, password);
        return new KafkaMonitorTask.Builder()
                .metricPrefix(configuration.getMetricPrefix())
                .metricWriter(configuration.getMetricWriter())
                .jmxConnectionAdapter(adapter)
                .server(server)
                .mbeans((List<Map>) configuration.getConfigYml().get(MBEANS))
                .build();
    }

    private String getPassword(Map server) {
        String password = convertToString(server.get(PASSWORD), "");
        if (!Strings.isNullOrEmpty(password)) {
            return password;
        }
        String encryptionKey = convertToString(configuration.getConfigYml().get(ConfigConstants.ENCRYPTION_KEY), "");
        String encryptedPassword = convertToString(server.get(ENCRYPTED_PASSWORD), "");
        if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
            Map<String, String> cryptoMap = Maps.newHashMap();
            cryptoMap.put(PASSWORD_ENCRYPTED, encryptedPassword);
            cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
            return CryptoUtil.getPassword(cryptoMap);
        }
        return null;
    }

    private static String getImplementationVersion() {
        return KafkaMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);
        logger.getRootLogger().addAppender(ca);

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/Users/Muddam/AppDynamics/Code/extensions/kafka-monitoring-extension/src/main/resources/config/config.yml");

        final KafkaMonitor monitor = new KafkaMonitor();
        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the Task ", e);
                }
            }
        }, 2, 60, TimeUnit.SECONDS);
    }
}
