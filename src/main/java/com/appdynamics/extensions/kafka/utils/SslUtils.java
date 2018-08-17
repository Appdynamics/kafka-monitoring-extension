package com.appdynamics.extensions.kafka.utils;

import com.appdynamics.extensions.crypto.Decryptor;
import com.appdynamics.extensions.kafka.KafkaMonitor;
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class SslUtils {
    private static final Logger logger = LoggerFactory.getLogger(SslUtils.class);

    public void setSslProperties(Map<String, ?> configMap){

        //if the config yaml contains the field sslTrustStorePath then the keys are set
        // if the field is not present, it defaults to <MA HOME>/conf/
        // any change in config.yml parameters requires a MA restart
        if(configMap.containsKey(Constants.CONNECTION)) {
            Map<String, ?> connectionMap = (Map<String, ?>) configMap.get(Constants.CONNECTION);
            if (connectionMap.containsKey(Constants.TRUST_STORE_PATH) &&
                    !Strings.isNullOrEmpty(connectionMap.get(Constants.TRUST_STORE_PATH).toString())) {
                String sslTrustStorePath = connectionMap.get(Constants.TRUST_STORE_PATH).toString();
                File customSslTrustStoreFile = new File(sslTrustStorePath);
                if(customSslTrustStoreFile == null || !customSslTrustStoreFile.exists() ) {
                    logger.debug("The file [{}] doesn't exist", customSslTrustStoreFile.getAbsolutePath());
                }
                else {
                    logger.debug("Using custom SSL truststore [{}]", sslTrustStorePath);
                    System.setProperty("javax.net.ssl.trustStore", customSslTrustStoreFile.getAbsolutePath());
                }
            }
            else if (connectionMap.containsKey(Constants.TRUST_STORE_PATH) &&
                    Strings.isNullOrEmpty(connectionMap.get(Constants.TRUST_STORE_PATH).toString())){
                //getting path of machine agent home
                File installDir = PathResolver.resolveDirectory(AManagedMonitor.class);
                File defaultTrustStoreFile = PathResolver.getFile("conf/cacerts.jks", installDir);
                if (defaultTrustStoreFile == null || !defaultTrustStoreFile.exists()) {
                    logger.debug("The file [{}] doesn't exist", installDir+"/conf/cacerts.jks");
                }
                else {
                    logger.debug("Using Machine Agent truststore {}", installDir+"conf/cacerts.jks");
                    System.setProperty("javax.net.ssl.trustStore", defaultTrustStoreFile.getAbsolutePath());
                }
            }
            System.setProperty("javax.net.ssl.trustStorePassword", getSslTrustStorePassword(connectionMap, configMap));
        }
    }

    private String getSslTrustStorePassword(Map<String, ?> connectionMap, Map<String, ?> config) {
        String password = (String) connectionMap.get("sslTrustStorePassword");
        if (!Strings.isNullOrEmpty(password)) {
            return password;
        } else {
            String encrypted = (String) connectionMap.get("sslTrustStoreEncryptedPassword");
            if (!Strings.isNullOrEmpty(encrypted)) {
                String encryptionKey = (String) config.get("encryptionKey");
                if (!Strings.isNullOrEmpty(encryptionKey)) {
                    return new Decryptor(encryptionKey).decrypt(encrypted);
                } else {
                    logger.error("Cannot decrypt the password. Encryption key not set");
                    throw new RuntimeException("Cannot decrypt [encryptedPassword], since [encryptionKey] is not set");
                }
            } else {
                logger.warn("No password set, using empty string");
                return "";
            }
        }
    }
}
