package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.http.Http4ClientBuilder;
import com.appdynamics.extensions.kafka.metrics.CustomSSLSocketFactoryTest;
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaMonitorTest {


    public static final String CONFIG_ARG = "config-file";
    MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration("Kafka", "Custom Metrics|Kafka|", PathResolver.resolveDirectory(AManagedMonitor.class), Mockito.mock(AMonitorJob.class));

    @Test
    public void testKafkaMonitorExtension() throws TaskExecutionException {
        KafkaMonitor kafkaMonitor = new KafkaMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "/Users/vishaka.sekar/AppDynamics/kafka-monitoring-extension/src/test/resources/conf/config.yml");
        kafkaMonitor.execute(taskArgs, null);

    }

//    @Test(expected = Exception.class)
//    public void testConfigureSSL() throws Exception {
//        int port = 8745;
//        JMXConnectorServer jmxConnectorServer = CustomSSLSocketFactoryTest.startSSL(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+port+"/jmxrmi");
//        Map env = new HashMap();
////        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,new CustomSSLSocketFactory());
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//    }

//    @Test
//    public void testConfigureSSLwithKeys() throws Exception {
//        int port = 8745;
//        JMXConnectorServer jmxConnectorServer = CustomSSLSocketFactoryTest.startSSL(port);
//        jmxConnectorServer.start();
//        JMXServiceURL serviceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+port+"/jmxrmi");
////        System.setProperty("javax.net.ssl.trustStore", "/Users/vishaka.sekar/AppDynamics/client/kafka.client.truststore.jks");
////        System.setProperty("javax.net.ssl.trustStorePassword", "test1234");
//        String truststore = "/Users/vishaka.sekar/AppDynamics/client/kafka.client.truststore.jks";
//        char truststorepass[] = "test1234".toCharArray();
//        KeyStore ks = KeyStore.getInstance("JKS");
//        ks.load(new FileInputStream(truststore), truststorepass);
//        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//        tmf.init(ks);
//        SSLContext ctx = SSLContext.getInstance("TLS");
//        ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
//
//        Map env = new HashMap();
//        CustomSSLSocketFactory  customSSLSocketFactory = new CustomSSLSocketFactory();
//        SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
//        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,ctx.getSocketFactory());
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,   customSSLSocketFactory);
//        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env);
//        jmxConnectorServer.stop();
//    }


}