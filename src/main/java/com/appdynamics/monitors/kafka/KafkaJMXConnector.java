/**
 * Copyright 2014 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.monitors.kafka;


import com.appdynamics.monitors.kafka.config.Server;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.log4j.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KafkaJMXConnector {

    private static final String JMX_URL = "service:jmx:rmi:///jndi/rmi://${HOST}:${PORT}/jmxrmi";
    private static final Logger logger = Logger.getLogger(KafkaJMXConnector.class);


    public static JMXConnector connect(Server server) throws TaskExecutionException {

        try {
            JMXServiceURL url = new JMXServiceURL(getJMXServiceURL(server.getHost(), server.getPort()));
            final Map<String, Object> env = new HashMap<String, Object>();
            if (!Strings.isNullOrEmpty(server.getUsername())) {
                env.put(JMXConnector.CREDENTIALS, new String[]{server.getUsername(), server.getPassword()});
                return JMXConnectorFactory.connect(url, env);
            } else {
                return JMXConnectorFactory.connect(url);
            }
        } catch (IOException e) {
            logger.error("Error while connecting to JMX Server", e);
            throw new TaskExecutionException("Error while connecting to JMX Server", e);
        }
    }

    public static Set<ObjectInstance> queryMBeans(JMXConnector connector, String domain) {
        MBeanServerConnection connection = null;
        if (connector == null) {
            throw new IllegalArgumentException("JMXConnector should not be null");
        }
        try {
            connection = connector.getMBeanServerConnection();
            Set<ObjectInstance> allMbeans = connection.queryMBeans(new ObjectName(domain+":*"), null);
            return allMbeans;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (MalformedObjectNameException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private static String getJMXServiceURL(String host, int port) {
        Map<String, String> args = new HashMap<String, String>();
        args.put("HOST", host);
        args.put("PORT", String.valueOf(port));
        StrSubstitutor strSubstitutor = new StrSubstitutor(args);
        return strSubstitutor.replace(JMX_URL);
    }

    public static void close(JMXConnector connector) {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static MBeanAttributeInfo[] fetchAllAttributesForMbean(JMXConnector connector, ObjectName objectName) {
        try {
            MBeanAttributeInfo[] attributes = connector.getMBeanServerConnection().getMBeanInfo(objectName).getAttributes();
            return attributes;
        } catch (InstanceNotFoundException e) {
            logger.error("Unable to fetch Attributes For " + objectName);
        } catch (IntrospectionException e) {
            logger.error("Unable to fetch Attributes For " + objectName);
        } catch (ReflectionException e) {
            logger.error("Unable to fetch Attributes For " + objectName);
        } catch (IOException e) {
            logger.error("Unable to fetch Attributes For " + objectName);
        }
        return null;
    }

    public static Object getMBeanAttribute(JMXConnector connector, ObjectName objectName, String name) {
        try {
            return connector.getMBeanServerConnection().getAttribute(objectName, name);
        } catch (MBeanException e) {
            logger.error("Unable to fetch Mbeans Info " + objectName + "attrName=" + name + e);
        } catch (AttributeNotFoundException e) {
            logger.error("Unable to fetch Mbeans Info " + objectName + "attrName=" + name + e);
        } catch (InstanceNotFoundException e) {
            logger.error("Unable to fetch Mbeans Info " + objectName + "attrName=" + name + e);
        } catch (ReflectionException e) {
            logger.error("Unable to fetch Mbeans Info " + objectName + "attrName=" + name + e);
        } catch (IOException e) {
            logger.error("Unable to fetch Mbeans Info " + objectName + "attrName=" + name + e);
        }
        return null;
    }
}
