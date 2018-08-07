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

package com.appdynamics.extensions.kafka;

import com.appdynamics.extensions.crypto.Decryptor;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.*;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Map;

public class CustomSSLSocketFactory extends SslRMIClientSocketFactory {

    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactory.class);

    public SSLSocketFactory createSocketFactory(Map<String, ?> connectionMap) throws IOException {
        String trustStorePath = connectionMap.get("sslTrustStorePath").toString();
        char[] trustStorePassword = getTrustStorePassword(connectionMap.get("encryptionKey").toString(), connectionMap);
        try {
                //initializing truststore
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(new FileInputStream(trustStorePath), trustStorePassword);
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);

                //create ssl Context of the protocol specified in config.yml
                SSLContext sslContext = SSLContext.getInstance(connectionMap.get("sslProtocol").toString());
                //initialize with the trust managers
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                return sslContext.getSocketFactory();

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