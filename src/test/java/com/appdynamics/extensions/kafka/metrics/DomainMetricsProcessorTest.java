package com.appdynamics.extensions.kafka.metrics;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.conf.modules.MetricCharSequenceReplaceModule;
import com.appdynamics.extensions.kafka.JMXConnectionAdapter;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricCharSequenceReplacer;
import com.appdynamics.extensions.util.MetricPathUtils;
import com.appdynamics.extensions.util.PathResolver;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.management.*;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class DomainMetricsProcessorTest {

    @Test
    public void whenNonCompositeObjectsThenReturnMetrics()  throws MalformedObjectNameException, ReflectionException,
    InstanceNotFoundException,IntrospectionException,IOException {

        JMXConnector jmxConnector = mock(JMXConnector.class);
        JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);
        MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
        MonitorContext monitorContext = mock(MonitorContext.class);
        MonitorContextConfiguration configuration = mock(MonitorContextConfiguration.class);
        ABaseMonitor kafkaMonitor = mock(ABaseMonitor.class);
        when(kafkaMonitor.getContextConfiguration()).thenReturn(configuration);
        when(configuration.getContext()).thenReturn(monitorContext);
        MetricPathUtils.registerMetricCharSequenceReplacer(kafkaMonitor);
        Map<String, ?> conf = YmlReader.readFromFile(new File("/Users/vishaka.sekar/AppDynamics/kafka-monitoring-extension-ci/src/test/resources/conf/config_composite_and_non_composite_metrics.yml"));
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
        List<Map> mBeans = (List<Map>) config.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance(
                "org.apache.kafka.server:type=ReplicaManager,name=IsrExpandsPerSec", "test"));

        List<Attribute> attributes = Lists.newArrayList();
        attributes.add(new Attribute("Count", 100));
        attributes.add(new Attribute("Mean Rate", 200 ));
        List<String> metricNames = Lists.newArrayList();


        doReturn(objectInstances).when(jmxConnectionAdapter).queryMBeans(eq(jmxConnector),
                Mockito.any(ObjectName.class) );
        doReturn(metricNames).when(jmxConnectionAdapter).getReadableAttributeNames(eq(jmxConnector),
                Mockito.any(ObjectInstance.class));
        doReturn(attributes).when(jmxConnectionAdapter).getAttributes(eq(jmxConnector), Mockito.any(ObjectName.class),
                Mockito.any(String[].class));
        DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor(
                contextConfiguration, jmxConnectionAdapter,
                jmxConnector, "server1", metricWriteHelper);

        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        domainMetricsProcessor.populateMetricsForMBean(mBeans.get(0));
        verify(metricWriteHelper).transformAndPrintMetrics(pathCaptor.capture());
        Metric firstResultMetric = (Metric)pathCaptor.getValue().get(0);
        Metric secondResultMetric = (Metric)pathCaptor.getValue().get(1);
        Assert.assertEquals(firstResultMetric.getMetricName(),"Count");
        Assert.assertEquals(firstResultMetric.getMetricValue(), "100");
        Assert.assertEquals(firstResultMetric.getAggregationType(), "AVERAGE");
        Assert.assertEquals(firstResultMetric.getClusterRollUpType(), "INDIVIDUAL");
        Assert.assertEquals(firstResultMetric.getTimeRollUpType(), "AVERAGE");
        Assert.assertEquals(secondResultMetric.getMetricName(), "Mean Rate");
        Assert.assertEquals(secondResultMetric.getMetricValue(), "200");
        Assert.assertEquals(secondResultMetric.getAggregationType(), "AVERAGE");
        Assert.assertEquals(secondResultMetric.getClusterRollUpType(), "INDIVIDUAL");
        Assert.assertEquals(secondResultMetric.getTimeRollUpType(), "AVERAGE");

    }

    @Test
    public void whenCompositeObjectsThenReturnMetrics() throws MalformedObjectNameException, ReflectionException,
            InstanceNotFoundException,IntrospectionException,IOException,OpenDataException {

        JMXConnector jmxConnector = mock(JMXConnector.class);
        JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);
        MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
        MonitorContext monitorContext = mock(MonitorContext.class);
        MonitorContextConfiguration configuration = mock(MonitorContextConfiguration.class);
        ABaseMonitor kafkaMonitor = mock(ABaseMonitor.class);
        when(kafkaMonitor.getContextConfiguration()).thenReturn(configuration);
        when(configuration.getContext()).thenReturn(monitorContext);
        MetricPathUtils.registerMetricCharSequenceReplacer(kafkaMonitor);
        Map<String, ?> conf = YmlReader.readFromFile(new File("src/test/resources/conf/config_for_composite_metrics.yml"));
        MetricCharSequenceReplaceModule metricCharSequenceReplaceModule = new MetricCharSequenceReplaceModule();
        metricCharSequenceReplaceModule.initMetricCharSequenceReplacer(conf);
        when(monitorContext.getMetricCharSequenceReplacer()).thenReturn(metricCharSequenceReplaceModule.getMetricCharSequenceReplacer());

        MetricCharSequenceReplacer replacer = mock(MetricCharSequenceReplacer.class);
        when(monitorContext.getMetricCharSequenceReplacer()).thenReturn(replacer);
        when(MetricPathUtils.getReplacedString("HeapMemoryUsage.min")).thenReturn("HeapMemoryUsage Min");

        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        List<Map<String, ?>> mBeans = (List<Map<String, ?>>) conf.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance("java.lang:type=Memory", "test"));
        List<Attribute> attributes = Lists.newArrayList();
        attributes.add(new Attribute("HeapMemoryUsage", createCompositeDataSupportObject()));

        List<String> metricNames = Lists.newArrayList();

        doReturn(objectInstances).when(jmxConnectionAdapter).queryMBeans(eq(jmxConnector),Mockito.any(ObjectName.class) );
        doReturn(metricNames).when(jmxConnectionAdapter).getReadableAttributeNames(eq(jmxConnector), Mockito.any(ObjectInstance.class));
        doReturn(attributes).when(jmxConnectionAdapter).getAttributes(eq(jmxConnector), Mockito.any(ObjectName.class), Mockito.any(String[]
                .class));
        DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor(
                configuration, jmxConnectionAdapter,
                jmxConnector, "server2", metricWriteHelper);
        for (Map mBean : mBeans) {

                domainMetricsProcessor.populateMetricsForMBean(mBean);
                verify(metricWriteHelper)
                        .transformAndPrintMetrics(pathCaptor.capture());
                Metric firstResultMetric = (Metric)pathCaptor.getValue().get(0);
                Assert.assertEquals(firstResultMetric.getMetricName(),"HeapMemoryUsage Min");
                Assert.assertEquals(firstResultMetric.getMetricValue(), "50");
                Assert.assertEquals(firstResultMetric.getAggregationType(), "AVERAGE");
                Assert.assertEquals(firstResultMetric.getClusterRollUpType(), "INDIVIDUAL");
                Assert.assertEquals(firstResultMetric.getTimeRollUpType(), "AVERAGE");
        }
    }

    @Test
    public void whenCompositeAndNonCompositeObjectsThenReturnMetrics() throws IOException,
            IntrospectionException,ReflectionException, InstanceNotFoundException,MalformedObjectNameException,
            OpenDataException{

        JMXConnector jmxConnector = mock(JMXConnector.class);
        JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);
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
        when(MetricPathUtils.getReplacedString("HeapMemoryUsage.min")).thenReturn("HeapMemoryUsage Min");
        when(MetricPathUtils.getReplacedString("HeapMemoryUsage.max")).thenReturn("HeapMemoryUsage Max");
        when(MetricPathUtils.getReplacedString("Count")).thenReturn("Count");
        when(MetricPathUtils.getReplacedString("IsrExpandsPerSec")).thenReturn("IsrExpandsPerSec Min");

        List<Map<String, ?>> mBeans = (List<Map<String, ?>>) conf.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance(
                "org.apache.kafka.server:type=ReplicaManager,name=IsrExpandsPerSec", "test"));
        List<Attribute> attributes = Lists.newArrayList();
        attributes.add(new Attribute(("Count"), 0));
        attributes.add(new Attribute("HeapMemoryUsage", createCompositeDataSupportObject()));
        List<String> metricNames = Lists.newArrayList();

        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        doReturn(objectInstances).when(jmxConnectionAdapter).queryMBeans(eq(jmxConnector)
                ,Mockito.any(ObjectName.class) );
        doReturn(metricNames).when(jmxConnectionAdapter).getReadableAttributeNames(eq(jmxConnector)
                , Mockito.any(ObjectInstance.class));
        doReturn(attributes).when(jmxConnectionAdapter).getAttributes(eq(jmxConnector),
                Mockito.any(ObjectName.class), Mockito.any(String[]
                .class));
        DomainMetricsProcessor domainMetricsProcessor = new DomainMetricsProcessor(
                configuration, jmxConnectionAdapter,
                jmxConnector,"server1", metricWriteHelper);
        for (Map mBean : mBeans) {

                domainMetricsProcessor.populateMetricsForMBean(mBean);
                verify(metricWriteHelper)
                        .transformAndPrintMetrics(pathCaptor.capture());
                System.out.println(pathCaptor.getAllValues());
                Metric firstResultMetric = (Metric) pathCaptor.getValue().get(0);
                Metric secondResultMetric = (Metric) pathCaptor.getValue().get(1);
                Assert.assertEquals(firstResultMetric.getMetricName(), "Count");
                Assert.assertEquals(firstResultMetric.getMetricValue(), "0");
                Assert.assertEquals(firstResultMetric.getAggregationType(), "AVERAGE");
                Assert.assertEquals(firstResultMetric.getClusterRollUpType(), "INDIVIDUAL");
                Assert.assertEquals(firstResultMetric.getTimeRollUpType(), "AVERAGE");
                Assert.assertEquals(secondResultMetric.getMetricName(), "HeapMemoryUsage Min");
                Assert.assertEquals(secondResultMetric.getMetricValue(), "50");
                Assert.assertEquals(secondResultMetric.getAggregationType(), "AVERAGE");
                Assert.assertEquals(secondResultMetric.getClusterRollUpType(), "INDIVIDUAL");
                Assert.assertEquals(secondResultMetric.getTimeRollUpType(), "SUM");
        }
    }

    private CompositeDataSupport createCompositeDataSupportObject () throws OpenDataException {
        String typeName = "type";
        String description = "description";
        String[] itemNames = {"min", "max"};
        String[] itemDescriptions = {"maxDesc", "minDesc"};
        OpenType<?>[] itemTypes = new OpenType[]{new OpenType("java.lang.String", "type",
                "description") {
            @Override
            public boolean isValue (Object obj) {
                return true;
            }
            @Override
            public boolean equals (Object obj) {
                return false;
            }
            @Override
            public int hashCode () {
                return 0;
            }
            @Override
            public String toString () {
                return "50";
            }
        }, new OpenType("java.lang.String", "type", "description") {
            @Override
            public boolean isValue (Object obj) {
                return true;
            }
            @Override
            public boolean equals (Object obj) {
                return false;
            }
            @Override
            public int hashCode () {
                return 0;
            }
            @Override
            public String toString () {
                return "100";
            }
        }};
        CompositeType compositeType = new CompositeType(typeName, description, itemNames,
                itemDescriptions, itemTypes);
        String[] itemNamesForCompositeDataSupport = {"min", "max"};
        Object[] itemValuesForCompositeDataSupport = {new BigDecimal(50), new BigDecimal(100)};
        return new CompositeDataSupport(compositeType, itemNamesForCompositeDataSupport,
                itemValuesForCompositeDataSupport);
    }
}

