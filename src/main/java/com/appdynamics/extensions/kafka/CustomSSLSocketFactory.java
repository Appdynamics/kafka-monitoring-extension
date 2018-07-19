package com.appdynamics.extensions.kafka;

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

    public SSLSocketFactory createSocketFactory() throws IOException {

        //TODO:refactor this file
        //TODO:re-name variables
        String trustStorePath = "";
        char trustStorePassword[] = "".toCharArray();//todo:check char-set
        SSLSocketFactory ssf = null;
        try {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream(truststore), truststorepass);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                SSLContext ctx = SSLContext.getInstance("TLS");//todo: take it from config
                ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
                ssf = ctx.getSocketFactory();
            return ssf;

        }catch(NoSuchAlgorithmException exception){
            logger.debug("No Such algorithm");
        } catch (CertificateException e) {
//            e.printStackTrace();
            //todo: logger.error
        }

        return null;
    }


}
