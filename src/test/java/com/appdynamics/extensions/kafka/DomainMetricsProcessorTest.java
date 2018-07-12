package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.kafka.metrics.DomainMetricsProcessor;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import javax.management.*;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Phaser;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DomainMetricsProcessorTest {
    JMXConnector jmxConnector = mock(JMXConnector.class);
    JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);

    @Test
    @SuppressWarnings("unchecked")
    public void getNodeMetrics() throws IOException,IntrospectionException,ReflectionException, InstanceNotFoundException,MalformedObjectNameException {
        MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
        MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration("Kafka", "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class), Mockito.mock(AMonitorJob.class));
        contextConfiguration.setConfigYml("/Users/vishaka.sekar/AppDynamics/kafka-monitoring-extension/src/test/resources/conf/config_for_metrics.yml");

        Map config = contextConfiguration.getConfigYml();
        List<Map> mBeans = (List) config.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance("org.apache.kafka.server:type=ReplicaManager,name=IsrExpandsPerSec", "test"));

        List<Attribute> attributes = Lists.newArrayList();
        attributes.add(new Attribute("Count", 100));
        attributes.add(new Attribute("Value", 200 ));

        List<String> metricNames = Lists.newArrayList();
        metricNames.add("Count");
        metricNames.add("Value");

        when(jmxConnectionAdapter.queryMBeans(eq(jmxConnector), Mockito.any(ObjectName.class))).thenReturn(objectInstances);
        when(jmxConnectionAdapter.getReadableAttributeNames(eq(jmxConnector), Mockito.any(ObjectInstance.class))).thenReturn(metricNames);
        when(jmxConnectionAdapter.getAttributes(eq(jmxConnector), Mockito.any(ObjectName.class), Mockito.any(String[]
                .class))).thenReturn(attributes);

        Map<String, String> server = Maps.newHashMap();
        server.put("host", "localhost");
        server.put("port", "9999");
        server.put("displayName", "TestServer1");

        for(Map mBean : mBeans){
            Phaser phaser = new Phaser();
            phaser.register();
            Map<String, ?> metricProperties = (Map<String, ?>) mBean.get("metrics");
            DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor(contextConfiguration, jmxConnectionAdapter,
                    jmxConnector, mBean, server.get("displayName"), metricWriteHelper,phaser);
            List<Metric> metrics = domainMetricsProcessor.getNodeMetrics(jmxConnector, mBean.get("objectName").toString(),metricProperties );
            Assert.assertEquals(metrics.get(0).getMetricName(),"Count");
            Assert.assertEquals(metrics.get(1).getMetricName(), "Value");
        }

    }
}
