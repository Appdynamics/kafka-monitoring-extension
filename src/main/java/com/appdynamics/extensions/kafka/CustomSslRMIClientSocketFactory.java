package com.appdynamics.extensions.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.management.jmxremote.ConnectorBootstrap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class CustomSslRMIClientSocketFactory extends SslRMIClientSocketFactory {
    private static final Logger logger = LoggerFactory.getLogger(CustomSslRMIClientSocketFactory.class);

    @Override
    public Socket createSocket (String host, int port) throws IOException {
        
        String trustStorePath = "/Users/vishaka.sekar/AppDynamics/client/kafka.client.truststore.jks";
        char[] trustStorePassword = "test1234".toCharArray();
        try {
            //initializing truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(trustStorePath), trustStorePassword);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            //create ssl Context
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            //initialize with the trust managers
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            return sslContext.getSocketFactory().createSocket("127.0.0.1" , 9999);


        } catch (NoSuchAlgorithmException exception) {
            logger.error("No Such algorithm", exception);
        } catch (CertificateException e) {
            logger.error("CommonName in the certificate is not the same as the host name", e);
        } catch (KeyStoreException e) {
            logger.error("Cannot initialize trustStore", e);
        } catch (KeyManagementException e) {
            logger.error("Cannot initialize keyMangers", e);
        }
        return null;
    }

}
