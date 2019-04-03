package com.appdynamics.extensions.kafka;
import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.conf.modules.MetricCharSequenceReplaceModule;
import com.appdynamics.extensions.kafka.utils.Constants;
import com.appdynamics.extensions.metrics.MetricCharSequenceReplacer;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.MetricPathUtils;
import com.appdynamics.extensions.util.PathResolver;
import com.appdynamics.extensions.yml.YmlReader;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * @author: {Vishaka Sekar} on {3/8/19}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JMXConnectionAdapter.class)
@PowerMockIgnore({ "javax.net.ssl.*" })
public class JMXConnectionAdapterTest {


    @Test
    public void when_ConnectionStatusZero_ThenCheckHeartBeat_And_ConnectionClose() throws IOException {

        JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);
        PowerMockito.mockStatic(JMXConnectionAdapter.class);
        MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
        MonitorContext monitorContext = mock(MonitorContext.class);
        MonitorContextConfiguration configuration = mock(MonitorContextConfiguration.class);
        ABaseMonitor kafkaMonitor = mock(ABaseMonitor.class);
        when(kafkaMonitor.getContextConfiguration()).thenReturn(configuration);
        when(configuration.getContext()).thenReturn(monitorContext);
        MetricPathUtils.registerMetricCharSequenceReplacer(kafkaMonitor);
        Map<String, ?> conf = YmlReader.readFromFile(new File("src/test/resources/conf/config_composite_and_non_composite_metrics.yml"));
        MetricCharSequenceReplaceModule metricCharSequenceReplaceModule = new MetricCharSequenceReplaceModule();
        metricCharSequenceReplaceModule.initMetricCharSequenceReplacer(conf);
        when(monitorContext.getMetricCharSequenceReplacer()).thenReturn(metricCharSequenceReplaceModule.getMetricCharSequenceReplacer());

        MetricCharSequenceReplacer replacer = mock(MetricCharSequenceReplacer.class);
        when(monitorContext.getMetricCharSequenceReplacer()).thenReturn(replacer);
        when(MetricPathUtils.getReplacedString("Count")).thenReturn("Count");
        when(MetricPathUtils.getReplacedString("Mean Rate")).thenReturn("Mean Rate");

        MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration
                ("Kafka Monitor",
                        "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class),
                        Mockito.mock(AMonitorJob.class));

        contextConfiguration.setConfigYml("src/test/resources/conf/config_for_non_composite_metrics.yml");
        Map config = contextConfiguration.getConfigYml();
        List<Map> kafkaServers = (List<Map>)config.get("servers");

        when(JMXConnectionAdapter.create(anyMap())).thenReturn(jmxConnectionAdapter);
        when(jmxConnectionAdapter.open(anyMap())).thenReturn(null);
        TasksExecutionServiceProvider tasksExecutionServiceProvider = mock(TasksExecutionServiceProvider.class);
        when(tasksExecutionServiceProvider.getMetricWriteHelper()).thenReturn(metricWriteHelper);

        for (Map<String, String> kafkaServer : kafkaServers) {
            KafkaMonitorTask task = new KafkaMonitorTask(tasksExecutionServiceProvider,
                    contextConfiguration, kafkaServer);
            AssertUtils.assertNotNull(kafkaServer.get(Constants.DISPLAY_NAME),
                    "The displayName can not be null");
            task.populateAndPrintMetrics();
            ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
            verify(metricWriteHelper).printMetric(pathCaptor.capture(),pathCaptor.capture(),pathCaptor.capture(),
                    pathCaptor.capture(),pathCaptor.capture());

            List<String> heartBeatMetricPath = pathCaptor.getAllValues();
            Assert.assertEquals("Custom Metrics|Kafka|Test Kafka Server1|kafka.server|HeartBeat",heartBeatMetricPath.get(0));
            Assert.assertEquals("0",heartBeatMetricPath.get(1));
            Assert.assertEquals("AVERAGE",heartBeatMetricPath.get(2));
            Assert.assertEquals("AVERAGE",heartBeatMetricPath.get(3));
            Assert.assertEquals("INDIVIDUAL", heartBeatMetricPath.get(4));

            verify(jmxConnectionAdapter).close(any());

        }

    }

}
