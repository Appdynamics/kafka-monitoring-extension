package com.appdynamics.extensions.kafka;
import com.appdynamics.extensions.conf.processor.ConfigProcessor;
import com.appdynamics.extensions.controller.*;
import com.appdynamics.extensions.controller.apiservices.ControllerAPIService;
import com.appdynamics.extensions.controller.apiservices.ControllerAPIServiceFactory;
import com.appdynamics.extensions.controller.apiservices.CustomDashboardAPIService;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import java.io.File;
import java.util.Map;

import static com.appdynamics.extensions.Constants.ENCRYPTION_KEY;

/**
 * @author: {Vishaka Sekar} on {2/11/19}
 */
public class IntegrationTestUtils {

     static ControllerInfo controllerInfo;
     static ControllerClient controllerClient;
     static  ControllerAPIService controllerAPIService;
     static MetricAPIService metricAPIService;
     static CustomDashboardAPIService customDashboardAPIService;
     static final Logger logger = ExtensionsLoggerFactory.getLogger(IntegrationTestUtils.class);

    public static MetricAPIService setUpMetricAPIService (File configFile, File installDir){

        Map<String, ?> config = YmlReader.readFromFileAsMap(configFile);
        config = ConfigProcessor.process(config);
        Map controllerInfoMap = (Map) config.get("controllerInfo");
        if(controllerInfoMap == null) {
            controllerInfoMap = Maps.newHashMap();
        }
        controllerInfoMap.put(ENCRYPTION_KEY, config.get(ENCRYPTION_KEY));
        try {
            controllerInfo = ControllerInfoFactory.initialize(controllerInfoMap, installDir);
            logger.info("Initialized ControllerInfo");
            ControllerInfoValidator controllerInfoValidator = new ControllerInfoValidator(controllerInfo);
            if (controllerInfoValidator.isValidated()) {
                controllerClient = ControllerClientFactory.initialize(controllerInfo,
                        (Map<String, ?>) config.get("connection"), (Map<String, ?>) config.get("proxy"),
                        (String) config.get(ENCRYPTION_KEY));
                logger.debug("Initialized ControllerClient");
                controllerAPIService = ControllerAPIServiceFactory.initialize(controllerInfo, controllerClient);
                logger.debug("Initialized ControllerAPIService");
                metricAPIService = controllerAPIService.getMetricAPIService();
                logger.debug("Initialized metricAPIService");
                return metricAPIService;
            }
            logger.warn("ControllerInfo instance is not validated and resolved.....the ControllerClient and ControllerAPIService are null");
        } catch (Exception e) {
            logger.error("Unable to initialize the ControllerModule properly.....the ControllerClient and ControllerAPIService will be set to null", e);
        }

        //TODO: check if this is ok
        return null;
    }


    public static CustomDashboardAPIService setUpCustomDashBoardAPIService (File configFile, File installDir){

        Map<String, ?> config = YmlReader.readFromFileAsMap(configFile);
        config = ConfigProcessor.process(config);
        Map controllerInfoMap = (Map) config.get("controllerInfo");
        if(controllerInfoMap == null) {
            controllerInfoMap = Maps.newHashMap();
        }
        controllerInfoMap.put(ENCRYPTION_KEY, config.get(ENCRYPTION_KEY));
        try {
            controllerInfo = ControllerInfoFactory.initialize(controllerInfoMap, installDir);
            logger.info("Initialized ControllerInfo");
            ControllerInfoValidator controllerInfoValidator = new ControllerInfoValidator(controllerInfo);
            if (controllerInfoValidator.isValidated()) {
                controllerClient = ControllerClientFactory.initialize(controllerInfo,
                        (Map<String, ?>) config.get("connection"), (Map<String, ?>) config.get("proxy"),
                        (String) config.get(ENCRYPTION_KEY));
                logger.debug("Initialized ControllerClient");
                controllerAPIService = ControllerAPIServiceFactory.initialize(controllerInfo, controllerClient);
                logger.debug("Initialized ControllerAPIService");
                customDashboardAPIService = controllerAPIService.getCustomDashboardAPIService();
                logger.debug("Initialized customDashboardAPIService");
                return customDashboardAPIService;
            }
            logger.warn("ControllerInfo instance is not validated and resolved.....the ControllerClient and ControllerAPIService are null");
        } catch (Exception e) {
            logger.error("Unable to initialize the ControllerModule properly.....the ControllerClient and ControllerAPIService will be set to null", e);
        }

        //TODO: check if this is ok
        return null;
    }

}
