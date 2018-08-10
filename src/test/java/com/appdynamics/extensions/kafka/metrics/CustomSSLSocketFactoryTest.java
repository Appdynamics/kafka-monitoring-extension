/**
 * Copyright 2018 AppDynamics, Inc.
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


package com.appdynamics.extensions.kafka.metrics;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.kafka.CustomSSLSocketFactory;
import com.appdynamics.extensions.util.PathResolver;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;

public  class CustomSSLSocketFactoryTest {
    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactoryTest.class);
//
//    public static JMXConnectorServer startDefaultSSLServer(int port) {
//        MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
//        HashMap env = new HashMap();
//        JMXConnectorServer jmxConnectorServer = null;
//        try {
//            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//            Registry registry = LocateRegistry.createRegistry(port, new SslRMIClientSocketFactory(), null);
//            jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mBeanServer);
//        } catch (Exception ioe) {
//            logger.debug("Could not connect");
//        }
//        return jmxConnectorServer;
//    }
//
//    @Test
//    public void testDefaultSSLServerConnection() throws Exception {
//        int port = 8750;
//        JMXConnectorServer jmxConnectorServer = startDefaultSSLServer(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//        Map env = new HashMap();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new SslRMIClientSocketFactory());
//        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, new SslRMIServerSocketFactory());
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//    }
//
//    @Test()
//    public void testDefaultSSLFactoryWithIncorrectJRETrustStore() throws Exception {
//        int port = 8747;
//        MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration
//                ("Kafka Monitor",
//                        "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class),
//                        Mockito.mock(AMonitorJob.class));
//        contextConfiguration.setConfigYml("src/test/resources/conf/config_for_SSL_with_default_jre_keys.yml");
//        Map env = new HashMap();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new SslRMIClientSocketFactory());
//        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, new SslRMIServerSocketFactory());
//        JMXConnectorServer jmxConnectorServer = CustomSSLSocketFactoryTest.startDefaultSSLServer(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//
//    }
////~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//    public static JMXConnectorServer startCustomSSLServer(int port) {
//        MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
//        HashMap env = new HashMap();
//        JMXConnectorServer jmxConnectorServer = null;
//        try {
//            MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration
//                    ("Kafka Monitor",
//                            "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class),
//                            Mockito.mock(AMonitorJob.class));
//            contextConfiguration.setConfigYml("src/test/resources/conf/config_for_SSL_server.yml");
//            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//            Map config = contextConfiguration.getConfigYml();
//            Map<String, String> connectionMap = (Map<String, String>) config.get("connection");
//            connectionMap.put("encryptionKey", "");
//            Registry registry = LocateRegistry.createRegistry(port,  new SslRMIClientSocketFactory(),null);
//            jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mBeanServer);
//        } catch (Exception ioe) {
//            logger.debug("Could not connect");
//        }
//        return jmxConnectorServer;
//    }
//
//    @Test
//    public void testCustomSSLServerConnection() throws Exception {
//        int port = 8745;
//        JMXConnectorServer jmxConnectorServer = startCustomSSLServer(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//        Map env = new HashMap();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new CustomSSLSocketFactory());
//        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, null);
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//    }
//
//    @Test
//    public void testCustomSSLFactoryWithCorrectTrustStore() throws Exception {
//        int port = 8746;
//        MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration
//                ("Kafka Monitor",
//                        "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class),
//                        Mockito.mock(AMonitorJob.class));
//        contextConfiguration.setConfigYml("src/test/resources/conf/config_for_SSL.yml");
//        Map config = contextConfiguration.getConfigYml();
//        Map<String, String> connectionMap = (Map<String, String>) config.get("connection");
//        connectionMap.put("encryptionKey", "");
//        Map env = new HashMap();
//        CustomSSLSocketFactory customSSLSocketFactory = new CustomSSLSocketFactory();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, customSSLSocketFactory.createSocketFactory(connectionMap));
//        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, new SslRMIServerSocketFactory());
//        JMXConnectorServer jmxConnectorServer = CustomSSLSocketFactoryTest.startCustomSSLServer(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//    }
//
//
//    @Test()
//    public void testCustomSSLFactoryWithIncorrectKeys() throws Exception {
//        int port = 8749;
//        MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration
//                ("Kafka Monitor",
//                        "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class),
//                        Mockito.mock(AMonitorJob.class));
//        contextConfiguration.setConfigYml("src/test/resources/conf/config_for_SSL_with_incorrect_keys.yml");
//        Map config = contextConfiguration.getConfigYml();
//        Map<String, String> connectionMap = (Map<String, String>) config.get("connection");
//        connectionMap.put("encryptionKey", "");
//        Map env = new HashMap();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new CustomSSLSocketFactory().createSocketFactory(connectionMap));
//        JMXConnectorServer jmxConnectorServer = CustomSSLSocketFactoryTest.startCustomSSLServer(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//    }

}