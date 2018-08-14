package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.kafka.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

public class ThreadLocalSslRmiClientSocketFactory implements RMIClientSocketFactory, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ThreadLocalSslRmiClientSocketFactory.class);

    public static final ThreadLocal<CustomSslRMIClientSocketFactory> SOCKET_FACTORY
            = new InheritableThreadLocal<CustomSslRMIClientSocketFactory>(){
        @Override
        protected CustomSslRMIClientSocketFactory initialValue(){
            return new CustomSslRMIClientSocketFactory();

        }
    };

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return SOCKET_FACTORY.get().createSocket(host, port);
    }

}
