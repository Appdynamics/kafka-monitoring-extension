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
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.*;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.*;
import java.security.cert.CertificateException;
import java.util.Map;

public class CustomSSLSocketFactory extends SslRMIClientSocketFactory {
    private static final Logger logger = LoggerFactory.getLogger(CustomSSLSocketFactory.class);
//todo: access specifiers
//todo: remove redundant method
    public SSLSocketFactory createSocketFactory(String encryptionKey, Map<String, ?> connectionMap) throws IOException {
        String trustStorePath = resolveTrustStorePath(connectionMap);
        char[] trustStorePassword = getTrustStorePassword(encryptionKey, connectionMap);
        String keyStorePath = resolveKeyStorePath(connectionMap);
        char[] keyStorePassword = getKeyStorePassword(encryptionKey, connectionMap);

        try {
                //initializing keystore
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(new FileInputStream(keyStorePath), keyStorePassword);
                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword);

                //initializing truststore
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(new FileInputStream(trustStorePath), trustStorePassword);
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);

                SSLContext sslContext = SSLContext.getInstance(connectionMap.get("sslProtocol").toString());
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
                return sslContext.getSocketFactory();

        } catch (NoSuchAlgorithmException exception) {
            logger.debug("No Such algorithm");
        } catch (CertificateException e) {
            logger.error("CommonName in the certificate is not the same as the host name: {}", e);
        } catch (KeyStoreException e) {
            logger.error("");
        } catch (KeyManagementException e) {
            logger.error("");
        }
         catch(UnrecoverableKeyException uke){

        }
        return null;
    }

    //todo: refactor this code to remove redundant methods
    SSLSocketFactory createSocketFactory(Map<String, ?> connectionMap) throws IOException {
        String trustStorePath = resolveTrustStorePath(connectionMap);
        char[] trustStorePassword = getTrustStorePassword("", connectionMap);
        String keyStorePath = resolveKeyStorePath(connectionMap);
        char[] keyStorePassword = getKeyStorePassword("",connectionMap);

        try {
                //initializing keystore
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(new FileInputStream(keyStorePath), keyStorePassword);
                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword);

                //initializing truststore
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(new FileInputStream(trustStorePath), trustStorePassword);
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                SSLContext sslContext = SSLContext.getInstance(connectionMap.get("sslProtocol").toString());
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
                        new SecureRandom());

                return sslContext.getSocketFactory();
        }catch(NoSuchAlgorithmException exception){
            logger.debug("No Such algorithm");
        } catch (CertificateException e) {
            logger.error("CommonName in the certificate is not the same as the host name: {}",e);
        } catch (KeyStoreException e) {
            logger.error("KeyStore exception {}", e);
        } catch (KeyManagementException e) {
            logger.error("KeyManagemena){
            logger.error("Unrecoverable Key {}",e);
        }
        return null;
    }

    //todo:chk path resolver if certs need to be in socketfactory directory
    String resolveTrustStorePath(Map<String, ?> connectionMap) {
        File installDir = PathResolver.resolveDirectory(CustomSSLSocketFactory.class);
        String sslTrustStorePath = (String) connectionMap.get("sslTrustStorePath");
        File file = PathResolver.getFile(sslTrustStorePath, installDir);
        logger.debug("The config property [sslTrustStorePath] with value [{}] is resolved to file [{}]"
                    , sslTrustStorePath, getPath(file));
        if (file == null || !file.exists()) {
            logger.debug("The sslTrustStorePath [{}] doesn't exist", getPath(file));
            return null;
        }
        return getPath(file).toString();
    }

    private static Object getPath(File file) { return file != null ? file.getAbsolutePath() : null; }

    protected static char[] getTrustStorePassword(String encryptionKey, Map<String, ?> connection) {
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

    String resolveKeyStorePath(Map<String, ?> connectionMap) {
        File installDir = PathResolver.resolveDirectory(CustomSSLSocketFactory.class);
        String sslTrustStorePath = (String) connectionMap.get("sslKeyStorePath");
        File file = PathResolver.getFile(sslTrustStorePath, installDir);
        logger.debug("The config property [sslTrustStorePath] with value [{}] is resolved to file [{}]"
                , sslTrustStorePath, getPath(file));
        if (file == null || !file.exists()) {
            logger.debug("The sslTrustStorePath [{}] doesn't exist", getPath(file));
            return null;
        }
        return getPath(file).toString();
    }

    //todo:change access specifiers to private
    protected static char[] getKeyStorePassword(String encryptionKey, Map<String, ?> connection) {
        String sslTrustStorePassword = (String) connection.get("sslKeyStorePassword");
        if (!Strings.isNullOrEmpty(sslTrustStorePassword)) {
            return sslTrustStorePassword.toCharArray();
        } else {
            String sslTrustStoreEncryptedPassword = (String) connection.get("sslKeyStoreEncryptedPassword");
            if (!Strings.isNullOrEmpty(sslTrustStoreEncryptedPassword) && !Strings.isNullOrEmpty(encryptionKey)) {
                return new Decryptor(encryptionKey).decrypt(sslTrustStoreEncryptedPassword).toCharArray();
            } else {
                logger.warn("Returning null password for sslKeyStore. Please set the [connection.sslKeyStore] or " +
                        "[connection.sslKeyStoreEncryptedPassword + encryptionKey]");
                return null;
            }
        }
    }

}