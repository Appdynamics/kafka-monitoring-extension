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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.management.remote.JMXServiceURL;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.StringTokenizer;

public class CustomSSLSocketFactory extends SslRMIClientSocketFactory {

    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactory.class);
    private Map<String, ?> connectionMap;

    public CustomSSLSocketFactory() {
    }

    public CustomSSLSocketFactory(Map<String,Object> connectionMap) {
        this.connectionMap = connectionMap;
    }

    public SSLSocketFactory createSocketFactory(Map<String, ?> connectionMap) throws IOException {
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

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        // Retrieve the SSLSocketFactory
        final SocketFactory sslSocketFactory = createSocketFactory(connectionMap);
        // Create the SSLSocket
        final SSLSocket sslSocket = (SSLSocket)
                sslSocketFactory.createSocket("127.0.0.1",
                        9999);
        // Set the SSLSocket Enabled Cipher Suites
        final String enabledCipherSuites =
                connectionMap.get("sslCipherSuites").toString();
        if (enabledCipherSuites != null) {
            StringTokenizer st = new StringTokenizer(enabledCipherSuites, ",");
            int tokens = st.countTokens();
            String enabledCipherSuitesList[] = new String[tokens];
            for (int i = 0 ; i < tokens; i++) {
                enabledCipherSuitesList[i] = st.nextToken();
            }
            try {
                sslSocket.setEnabledCipherSuites(enabledCipherSuitesList);
            } catch (IllegalArgumentException e) {
                throw (IOException)
                        new IOException(e.getMessage()).initCause(e);
            }
        }
        // Set the SSLSocket Enabled Protocols

        final String enabledProtocols =
                connectionMap.get("sslProtocols").toString();
        if (enabledProtocols != null) {
            StringTokenizer st = new StringTokenizer(enabledProtocols, ",");
            int tokens = st.countTokens();
            String enabledProtocolsList[] = new String[tokens];
            for (int i = 0 ; i < tokens; i++) {
                enabledProtocolsList[i] = st.nextToken();
            }
            try {
                sslSocket.setEnabledProtocols(enabledProtocolsList);
            } catch (IllegalArgumentException e) {
                throw (IOException)
                        new IOException(e.getMessage()).initCause(e);
            }
        }
        return sslSocket;
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return true;
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