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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public  class SSLConnectionTest {
//
//    @Before
//    public void setUpConnectionWithoutSSL(){
//
//        Properties props = new Properties();
//        props.setProperty("com.sun.management.jmxremote.authenticate", "false");
//        props.setProperty("com.sun.management.jmxremote.ssl", "false");
//        props.setProperty("com.sun.management.jmxremote.registry.ssl", "false");
//        JMXConnectorServer server = sun.management.jmxremote.ConnectorBootstrap
//                .startRemoteConnectorServer("9990", props);
//    }
//
//    @Test
//    public void whenNotUsingSslThenTestServerConnection() throws Exception {
//
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:9990/jmxrmi");
//        Map env = new HashMap();
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        Assert.assertNotNull(jmxConnector);
//    }

//    @Before
//    public void setUpConnectionWithSslAndCorrectKeys(){
//        System.setProperty("javax.net.ssl.trustStore", "/Users/vishaka.sekar/AppDynamics/client/kafka.client.truststore.jks");
//        System.setProperty("javax.net.ssl.trustStorePassword", "test1234");
//        System.setProperty("javax.net.ssl.keyStore", "/Users/vishaka.sekar/AppDynamics/server/kafka.server.keystore.jks");
//        System.setProperty("javax.net.ssl.keyStorePassword", "test1234");
//        System.setProperty("java.rmi.server.hostname", "10.0.0.106");
//        System.setProperty("com.sun.management.jmxremote.port", "6789");
//        Properties connectionProperties = new Properties();
//        connectionProperties.setProperty("com.sun.management.jmxremote.authenticate", "false");
//        connectionProperties.setProperty("com.sun.management.jmxremote.ssl", "true");
//        connectionProperties.setProperty("com.sun.management.jmxremote.registry.ssl", "true");
//
//       JMXConnectorServer server = sun.management.jmxremote.ConnectorBootstrap
//              .startRemoteConnectorServer("6789", connectionProperties);
//    }
//
//    @Test()
//    public void whenUsingSslAndCorrectKeysThenTestServerConnection() throws Exception {
//        int port = 6789;
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://10.0.0.106:6789/jmxrmi");
//        Map env = new HashMap();
//        env.put("com.sun.jndi.rmi.factory.socket",new SslRMIClientSocketFactory());
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new SslRMIClientSocketFactory());
//        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, new SslRMIServerSocketFactory());
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//    }

    @Before
    public void setUpConnectionWithIncorrectKeys(){
        System.setProperty("javax.net.ssl.trustStore", "/Users/vishaka.sekar/kafka.test.keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "test1234");
        System.setProperty("javax.net.ssl.keyStore", "/Users/vishaka.sekar/AppDynamics/server/kafka.server.keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "test1234");
        Properties props = new Properties();
        props.setProperty("com.sun.management.jmxremote.authenticate", "false");
        props.setProperty("com.sun.management.jmxremote.ssl", "true");
        props.setProperty("com.sun.management.jmxremote.registry.ssl", "false");
        JMXConnectorServer server = sun.management.jmxremote.ConnectorBootstrap
              .startRemoteConnectorServer("6789", props);
    }
    @Test(expected = Exception.class)
    public void testSSLServerConnectionWithIncorrectTrustStore() throws Exception {
        int port = 6789;
        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:6789/jmxrmi");
        Map env = new HashMap();
        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new SslRMIClientSocketFactory());
        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, new SslRMIServerSocketFactory());
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
    }
}