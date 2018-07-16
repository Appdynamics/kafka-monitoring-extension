package com.appdynamics.extensions.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class CustomSSLSocketFactory extends SslRMIClientSocketFactory {

    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactory.class);


    public SSLSocketFactory createSocketFactory() throws IOException {
        String truststore = "";
        char truststorepass[] = "".toCharArray();
        SSLSocketFactory ssf = null;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(truststore), truststorepass);
            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            ssf = ctx.getSocketFactory();

        }catch(NoSuchAlgorithmException exception){
            logger.debug("No Such algorithm");
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return ssf;
    }
}
