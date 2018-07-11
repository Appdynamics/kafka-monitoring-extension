package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.kafka.metrics.DomainMetricsProcessor;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.management.*;
import javax.management.remote.JMXConnector;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DomainMetricsProcessorTest {

    JMXConnector jmxConnector = mock(JMXConnector.class);
    JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);

    @Test

    public void getNodeMetrics() throws IOException,IntrospectionException,ReflectionException, InstanceNotFoundException,MalformedObjectNameException {

        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        AMonitorJob aMonitorJob = mock(AMonitorJob.class);
        //pass config file name
        MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Kafka Monitor", "Custom Metrics|Kafka|", aMonitorJob);
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/conf.yml");
        MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);

//        Map config = YmlReader.readFromFileAsMap(new File(this.getClass().getResource("/conf/config.yml").getFile()));
        List<Map> mBeans = (List) config.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance("kafka.server:type=BrokerTopicMetrics,*", "test"));

        Set<Attribute> attributes = Sets.newHashSet();
        attributes.add(new Attribute("Count", new Integer(100));
        attributes.add(new Attribute("Value", new Integer(200) );

        List<String> metricNames = Lists.newArrayList();
        metricNames.add("testmetric1");
        metricNames.add("testmetric2");

        when(jmxConnectionAdapter.queryMBeans(eq(jmxConnector), Mockito.any(ObjectName.class))).thenReturn(objectInstances);
        when(jmxConnectionAdapter.getReadableAttributeNames(eq(jmxConnector), Mockito.any(ObjectInstance.class))).thenReturn(metricNames);
        when(jmxConnectionAdapter.getAttributes(eq(jmxConnector), Mockito.any(ObjectName.class), Mockito.any(String[]
                .class))).thenReturn((List<Attribute>) eq(attributes));

        DomainMetricsProcessor domainMetricsProcessor = new





    }




}
