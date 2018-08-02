package com.appdynamics.extensions.kafka.metrics;

import com.appdynamics.extensions.kafka.CustomSSLSocketFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.RMIConnectorServer;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

public  class CustomSSLSocketFactoryTest {
    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactoryTest.class);

    public static JMXConnectorServer startSSL(int port) {
        MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
        HashMap env = new HashMap();
        JMXConnectorServer jmxConnectorServer = null;
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
            Registry registry = LocateRegistry.createRegistry(port, null, null);
            jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mBeanServer);
        } catch (Exception ioe) {
            logger.debug("Could not connect");
        }
        return jmxConnectorServer;
    }

    @Test
    public void testConfigureSSL() throws Exception {
        int port = 8745;
        JMXConnectorServer jmxConnectorServer = startSSL(port);
        jmxConnectorServer.start();
        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
        Map env = new HashMap();
        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new CustomSSLSocketFactory());
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
        jmxConnectorServer.stop();
    }

//    @Test
//    public void testCustomSSLFactoryWithKeys() throws Exception {
//        int port = 8745;
//        MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration
//                ("Kafka Monitor",
//                        "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class),
//                        Mockito.mock(AMonitorJob.class));
//        contextConfiguration.setConfigYml("src/test/resources/conf/config_for_SSL.yml");
//        Map config = contextConfiguration.getConfigYml();
//        Map<String, String> connectionMap = (Map<String, String>) config.get("connection");
//        Map env = new HashMap();
//        CustomSSLSocketFactory customSSLSocketFactory = new CustomSSLSocketFactory();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, customSSLSocketFactory.createSocketFactory("",
//          connectionMap));
//        JMXConnectorServer jmxConnectorServer = CustomSSLSocketFactoryTest.startSSL(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//    }
//
//    @Test
//    public void testDefaultSSLFactoryWithKeys() throws Exception {
//        int port = 8745;
//        MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration
//                ("Kafka Monitor",
//                        "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class),
//                        Mockito.mock(AMonitorJob.class));
//        contextConfiguration.setConfigYml("src/test/resources/conf/config_for_SSL.yml");
//        Map env = new HashMap();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, new SslRMIClientSocketFactory());
//        JMXConnectorServer jmxConnectorServer = CustomSSLSocketFactoryTest.startSSL(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//
//    }

}