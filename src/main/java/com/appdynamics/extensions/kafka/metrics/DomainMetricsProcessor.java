/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka.metrics;


import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.kafka.JMXConnectionAdapter;
import com.appdynamics.extensions.kafka.utils.Constants;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Phaser;

public class DomainMetricsProcessor implements Runnable{
    static final org.slf4j.Logger logger = LoggerFactory.getLogger(DomainMetricsProcessor.class);
    private final JMXConnectionAdapter jmxAdapter;
    private final JMXConnector jmxConnection;
    private MonitorContext context;
    private MetricWriteHelper metricWriteHelper;
    private List<Metric> metrics = new ArrayList<Metric>();
    private String metricPrefix;
    private Phaser phaser;
    private Map mbeanFromConfig;
    private String displayName;

    public DomainMetricsProcessor( JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnection, Map mbeanFromConfig, String displayName, MetricWriteHelper metricWriteHelper, String metricPrefix, Phaser phaser) {
        this.jmxAdapter = jmxAdapter;
        this.jmxConnection = jmxConnection;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = metricPrefix;
        this.phaser = phaser;
//        this.phaser.register();
        this.mbeanFromConfig = mbeanFromConfig;
        this.displayName = displayName;
    }

    @Override
    public void run() {
        try {
            Map<String, ?> metricProperties = (Map<String, ?>) this.mbeanFromConfig.get(Constants.METRICS);
            logger.debug(String.format("Processing metrics section from the conf file"));
            logger.debug("Size of metric section {}",metricProperties.size());
            List<String> mbeanNames = (List<String>) this.mbeanFromConfig.get("mbeanFullPath");
            logger.debug("Size of mbean section {}",mbeanNames.size());
            for (String mbeanName : mbeanNames) {
                    logger.debug(String.format("Processing mbean %s from the conf file", mbeanName));
                    List<Metric> finalMetricList = getNodeMetrics(jmxConnection,mbeanName,metricProperties);
                    logger.debug("Printing metrics for server {}", mbeanName);
                    metricWriteHelper.transformAndPrintMetrics(finalMetricList);
            }

            // heartbeat
        } catch (IntrospectionException | IOException | MalformedObjectNameException | InstanceNotFoundException | ReflectionException e) {
            e.printStackTrace();
        } finally {
            logger.debug("DomainProcessor Phaser arrived for {}", displayName);
//            phaser.arriveAndDeregister();
        }
    }

    private   List<Metric> getNodeMetrics(JMXConnector jmxConnection, String objectName, Map<String, ?> metricProperties) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException, MalformedObjectNameException {
        List<Metric> nodeMetrics = Lists.newArrayList();
        Set<ObjectInstance> objectInstances = this.jmxAdapter.queryMBeans(jmxConnection, ObjectName.getInstance(objectName));
        for (ObjectInstance instance : objectInstances) {
            List<String> metricNamesDictionary = this.jmxAdapter.getReadableAttributeNames(jmxConnection, instance);
            List<Attribute> attributes =this.jmxAdapter.getAttributes(jmxConnection, instance.getObjectName(), metricNamesDictionary.toArray(new String[metricNamesDictionary.size()]));
            collect(nodeMetrics, attributes, instance, metricProperties);
        }
        return nodeMetrics;
    }

    private void collect(List<Metric> nodeMetrics, List<Attribute> attributes, ObjectInstance instance, Map<String, ?> metricProperties) {
        for (Attribute attribute : attributes) {
            try {
                if(isCompositeDataObject(attribute)){
                    Set<String> attributesFound = ((CompositeData)attribute.getValue()).getCompositeType().keySet();
                    for(String str: attributesFound){
                        String key = attribute.getName()+ "."+ str;
                        Object attributeValue = ((CompositeDataSupport) attribute.getValue()).get(str);
                        if(metricProperties.containsKey(key)){
                            setMetricDetails(metricPrefix, key, attributeValue.toString(), instance, metricProperties, nodeMetrics);
                        }
                    }
                }
                else{
                    if(metricProperties.containsKey(attribute.getName())) {
                        setMetricDetails(metricPrefix, attribute.getName(), attribute.getValue(), instance, metricProperties, nodeMetrics);
                    }
                }
            } catch (Exception e) {
                logger.error("Error collecting value for {} {}", instance.getObjectName(), attribute.getName(), e);
            }
        }
    }

    private boolean isCompositeDataObject(Attribute attribute){
        return attribute.getValue().getClass().equals(CompositeDataSupport.class);
    }

    private void setMetricDetails (String metricPrefix, String attributeName, Object attributeValue, ObjectInstance instance, Map<String, ?> metricPropertiesMap, List<Metric> nodeMetrics) {
        String metricPath = metricPrefix + Constants.METRIC_SEPARATOR + buildName(instance)+ attributeName;
        Metric metric = new Metric(attributeName,  attributeValue.toString(), metricPath, metricPropertiesMap);
        nodeMetrics.add(metric);
    }

    private String buildName(ObjectInstance instance) {
        ObjectName objectName = instance.getObjectName();
        Hashtable<String, String> keyPropertyList = objectName.getKeyPropertyList();
        StringBuilder sb = new StringBuilder();
        String type = keyPropertyList.get("type");
        String name = keyPropertyList.get("name");

        sb.append(objectName.getDomain());
        if(!Strings.isNullOrEmpty(type)) {
            sb.append(Constants.METRIC_SEPARATOR);
            sb.append(type);
        }
        if(!Strings.isNullOrEmpty(name)) {
            sb.append(Constants.METRIC_SEPARATOR);
            sb.append(name);
        }
        sb.append(Constants.METRIC_SEPARATOR);
        keyPropertyList.remove("type");
        keyPropertyList.remove("name");
        for (Map.Entry<String, String> entry : keyPropertyList.entrySet()) {
            sb.append(entry.getKey()).append(Constants.METRIC_SEPARATOR).append(entry.getValue()).append(Constants.METRIC_SEPARATOR);
        }
        return sb.toString();
    }

}









