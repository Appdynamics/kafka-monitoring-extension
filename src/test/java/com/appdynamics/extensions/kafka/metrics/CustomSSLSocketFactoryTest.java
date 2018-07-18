package com.appdynamics.extensions.kafka.metrics;

import com.appdynamics.extensions.kafka.CustomSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;

public  class CustomSSLSocketFactoryTest {
    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactoryTest.class);

    public static JMXConnectorServer startSSL(int port) {

        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        HashMap env = new HashMap();
        SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
        SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
//        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,new CustomSSLSocketFactory());
//        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,ssf);
        JMXConnectorServer cs = null;
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+port+"/jmxrmi");
            Registry registry = LocateRegistry.createRegistry(port, csf, null);
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

        }catch(Exception ioe){
            logger.debug("could not connect");
        }

    return cs;

    }

}
