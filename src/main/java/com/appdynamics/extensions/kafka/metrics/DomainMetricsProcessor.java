/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.kafka.metrics;

import static com.appdynamics.extensions.kafka.utils.Constants.EXCLUDE;
import static com.appdynamics.extensions.kafka.utils.Constants.INCLUDE;
import static com.appdynamics.extensions.kafka.utils.Constants.METRICS;

import com.appdynamics.extensions.kafka.JMXConnectionAdapter;
import com.appdynamics.extensions.kafka.filters.ExcludeFilter;
import com.appdynamics.extensions.kafka.filters.IncludeFilter;
import com.appdynamics.extensions.kafka.utils.Constants;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DomainMetricsProcessor {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(DomainMetricsProcessor.class);
    private final JMXConnectionAdapter jmxAdapter;
    private final JMXConnector jmxConnection;

    public DomainMetricsProcessor(JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnection) {
        this.jmxAdapter = jmxAdapter;
        this.jmxConnection = jmxConnection;
    }

    public List<Metric> getNodeMetrics(String objectName, Map aConfigMBean) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException, MalformedObjectNameException {
        List<Metric> nodeMetrics = Lists.newArrayList();
        Set<ObjectInstance> objectInstances = jmxAdapter.queryMBeans(jmxConnection, ObjectName.getInstance(objectName));
        for (ObjectInstance instance : objectInstances) {
            List<String> metricNamesDictionary = jmxAdapter.getReadableAttributeNames(jmxConnection, instance);
            List<String> metricNamesToBeExtracted = applyFilters(aConfigMBean, metricNamesDictionary);
            List<Attribute> attributes = jmxAdapter.getAttributes(jmxConnection, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
            collect(nodeMetrics, attributes, instance);
        }
        return nodeMetrics;
    }

    private List<String> applyFilters(Map aConfigMBean, List<String> metricNamesDictionary) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        Set<String> filteredSet = Sets.newHashSet();
        Map configMetrics = (Map) aConfigMBean.get(METRICS);
        List includeDictionary = (List) configMetrics.get(Constants.INCLUDE);
        List excludeDictionary = (List) configMetrics.get(Constants.EXCLUDE);
        new ExcludeFilter(excludeDictionary).apply(filteredSet, metricNamesDictionary);
        new IncludeFilter(includeDictionary).apply(filteredSet, metricNamesDictionary);
        return Lists.newArrayList(filteredSet);
    }


    private void collect(List<Metric> nodeMetrics, List<Attribute> attributes, ObjectInstance instance) {
        for (Attribute attribute : attributes) {
            try {
                String attrName = attribute.getName();
                if(isCompositeDataObject(attribute)){
                    Set<String> attributesFound = ((CompositeData)attribute.getValue()).getCompositeType().keySet();
                    for(String str: attributesFound){
                        String key = attribute.getName()+ "."+ str;
                        Object attributeValue = ((CompositeDataSupport) attribute.getValue()).get(str);
//                        setMetricDetails(metricPrefix, key, attributeValue, instance, metricPropsPerMetricName, nodeMetrics);

                    }
                }
//                else
//                    setMetricDetails(metricPrefix, attribute.getName(), attribute.getValue(), instance, metricPropsPerMetricName, nodeMetrics);



            } catch (Exception e) {
                logger.error("Error collecting value for {} {}", instance.getObjectName(), attribute.getName(), e);
            }
        }
    }

    private boolean isCompositeDataObject(Attribute attribute){
        return attribute.getValue().getClass().equals(CompositeDataSupport.class);
    }
}
