package com.appdynamics.extensions.kafka.utils;

import com.appdynamics.extensions.crypto.Decryptor;
import com.appdynamics.extensions.kafka.CustomSSLSocketFactory;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

public class CustomSSLServerSocketFactory {

    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactory.class);
    private Map<String, ?> connectionMap;

    public CustomSSLServerSocketFactory(Map<String,Object> connectionMap) {
        this.connectionMap = connectionMap;
    }

    public SslRMIServerSocketFactory getServerSocketFactory() throws IOException {
        return new SslRMIServerSocketFactory(createSocketFactory(connectionMap),null,null,false);
    }

    public SSLContext createSocketFactory(Map<String, ?> connectionMap) throws IOException {
        String trustStorePath = connectionMap.get("sslTrustStorePath").toString();
        char[] trustStorePassword = getTrustStorePassword(connectionMap.get("encryptionKey").toString(), connectionMap);
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

            return sslContext;

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
    private static char[] getTrustStorePassword(String encryptionKey, Map<String, ?> connection) {
        String sslTrustStorePassword = (String) connection.get("sslTrustStorePassword");
        if (!Strings.isNullOrEmpty(sslTrustStorePassword)) {
            return sslTrustStorePassword.toCharArray();
        } else {
            String sslTrustStoreEncryptedPassword = (String) connection.get("sslTrustStoreEncryptedPassword");
            if (!Strings.isNullOrEmpty(sslTrustStoreEncryptedPassword) && !Strings.isNullOrEmpty(encryptionKey)) {
                return new Decryptor(encryptionKey).decrypt(sslTrustStoreEncryptedPassword).toCharArray();
            } else {
                logger.warn("Returning null password for sslTrustStore. Please set the [connection.sslTrustStore] or " +
                        "[connection.sslTrustStoreEncryptedPassword + encryptionKey]");
                return null;
            }
        }
    }

}
