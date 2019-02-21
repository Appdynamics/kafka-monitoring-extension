/*
 * Copyright 2018 AppDynamics, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.appdynamics.extensions.kafka.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
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

public class DomainMetricsProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DomainMetricsProcessor.class);
    private final JMXConnectionAdapter jmxAdapter;
    private final JMXConnector jmxConnection;
    private MetricWriteHelper metricWriteHelper;
    private String metricPrefix;

    public DomainMetricsProcessor (MonitorContextConfiguration configuration, JMXConnectionAdapter jmxAdapter,
                                   JMXConnector jmxConnection, String displayName, MetricWriteHelper metricWriteHelper) {
        this.jmxAdapter = jmxAdapter;
        this.jmxConnection = jmxConnection;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = configuration.getMetricPrefix() + Constants.METRIC_SEPARATOR + displayName;
    }

    public void populateMetricsForMBean (Map mbeanFromConfig) {
        try {
            String objectName = (String) mbeanFromConfig.get(Constants.OBJECTNAME);
            List<Map<String, ?>> mBeanMetricsFromConfig = (List<Map<String, ?>>) mbeanFromConfig.get(Constants.METRICS);
            logger.debug("Processing mbean {} ", objectName);
            List<Metric> finalMetricList = getMetricsForMBean(jmxConnection, objectName, mBeanMetricsFromConfig);
            logger.debug("Finished processing mbean {} ", objectName);
            metricWriteHelper.transformAndPrintMetrics(finalMetricList);
        } catch (IntrospectionException | IOException | MalformedObjectNameException
                | InstanceNotFoundException | ReflectionException e) {
            logger.error("Kafka Monitor error ", e);
        }
    }

    private List<Metric> getMetricsForMBean (JMXConnector jmxConnection, String objectName,
                                             List<Map<String, ?>> metricsListFromConfig)
            throws IntrospectionException, ReflectionException, InstanceNotFoundException,
            IOException, MalformedObjectNameException {
        Set<ObjectInstance> objectInstances = this.jmxAdapter.queryMBeans(jmxConnection,
                ObjectName.getInstance(objectName));
        List<Metric> metrics = new ArrayList<>();
        for (ObjectInstance object : objectInstances) {
            List<String> metricNamesDictionary = this.jmxAdapter.getReadableAttributeNames(jmxConnection, object);
            List<Attribute> attributes = this.jmxAdapter.getAttributes(jmxConnection,
                    object.getObjectName(), metricNamesDictionary.toArray(
                            new String[metricNamesDictionary.size()]));
            metrics.addAll((collectMetricsFromObject(attributes, object, metricsListFromConfig)));
        }
        return metrics;
    }

    private List<Metric> collectMetricsFromObject (List<Attribute> attributes, ObjectInstance instance,
                                                   List<Map<String, ?>> metricsListFromConfig) {
        List<Metric> metrics = Lists.newArrayList();
        for (Map<String, ?> metric : metricsListFromConfig) {
            for (Attribute attribute : attributes) {
                try {
                    if (isCompositeDataObject(attribute)) {
                        Set<String> attributesFound = ((CompositeData) attribute.getValue()).getCompositeType().keySet();
                        for (String str : attributesFound) {
                            String key = attribute.getName() + "." + str;
                            Object attributeValue = ((CompositeDataSupport) attribute.getValue()).get(str);
                            if (metric.containsKey(key)) {
                                metrics.add(setMetricDetails(metricPrefix, key, attributeValue, instance,
                                        (Map<String, String>) metric.get(key)));
                            }
                        }
                    } else {
                        if (metric.containsKey(attribute.getName())) {
                            Metric metricToBeAdded = setMetricDetails(metricPrefix, attribute.getName(), attribute.getValue(), instance,
                                    (Map<String, String>) metric.get(attribute.getName()));
                            metrics.add(metricToBeAdded);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error collecting value for {} {}", instance.getObjectName(), attribute.getName(), e);
                }
            }
        }
        return metrics;
    }

    private boolean isCompositeDataObject (Attribute attribute) {
        return attribute.getValue().getClass().equals(CompositeDataSupport.class);
    }

    private Metric setMetricDetails (String metricPrefix, String attributeName, Object attributeValue,
                                     ObjectInstance instance, Map<String, ?> metricPropertiesMap) {
        String token = null;
        token =   buildName(instance)+ attributeName;
        String[] tokens = token.split("\\|");
        Metric metric = new Metric(attributeName, String.valueOf(attributeValue), metricPropertiesMap, metricPrefix, tokens);
        return metric;

    }

    private String buildName (ObjectInstance instance) {
        ObjectName objectName = instance.getObjectName();
        Hashtable<String, String> keyPropertyList = objectName.getKeyPropertyList();
        StringBuilder sb = new StringBuilder();
        String type = keyPropertyList.get("type");
        String name = keyPropertyList.get("name");
        sb.append(objectName.getDomain());
        if (!Strings.isNullOrEmpty(type)) {
            sb.append(Constants.METRIC_SEPARATOR);
            sb.append(type);
        }
        if (!Strings.isNullOrEmpty(name)) {
            sb.append(Constants.METRIC_SEPARATOR);
            sb.append(name);
        }
        sb.append(Constants.METRIC_SEPARATOR);
        keyPropertyList.remove("type");
        keyPropertyList.remove("name");
        for (Map.Entry<String, String> entry : keyPropertyList.entrySet()) {
            sb.append(entry.getKey()).append(Constants.METRIC_SEPARATOR).append(entry.getValue())
                    .append(Constants.METRIC_SEPARATOR);
        }
        return sb.toString();
    }
}
