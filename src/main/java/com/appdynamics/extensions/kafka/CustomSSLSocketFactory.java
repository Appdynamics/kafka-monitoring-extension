package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.util.YmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class CustomSSLSocketFactory extends SslRMIClientSocketFactory {
    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactory.class);
    SSLSocketFactory createSocketFactory() throws IOException {
        String trustStorePath = "";
        char trustStorePassword[] = "".toCharArray();
        try {
                KeyStore keyStore = KeyStore.getInstance("JKS");
                keyStore.load(new FileInputStream(trustStorePath), trustStorePassword);
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");//todo: take it from config
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            return sslSocketFactory;

        }catch(NoSuchAlgorithmException exception){
            logger.debug("No Such algorithm");
        } catch (CertificateException e) {
            logger.error("CommonName in the certificate is not the same as the host name: {}",e);
        } catch (KeyStoreException e) {
            logger.error("");
        } catch (KeyManagementException e) {
           logger.error("");
        }
        return null;
    }
}
