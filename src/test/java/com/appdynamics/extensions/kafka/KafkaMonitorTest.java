package com.appdynamics.extensions.kafka;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

public class KafkaMonitorTest {


    public static final String CONFIG_ARG = "config-file";

    @Test
    public void testKafkaMonitorExtension() throws TaskExecutionException {
        KafkaMonitor kafkaMonitor = new KafkaMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "/Users/vishaka.sekar/AppDynamics/kafka-monitoring-extension/src/test/resources/conf/config.yml");
        kafkaMonitor.execute(taskArgs, null);

    }


}