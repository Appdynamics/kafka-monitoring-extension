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

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(IntegrationTestUtils.class);
    private static File installDir = new File("src/integration-test/resources/conf/");
    private static File configFile = new File("src/integration-test/resources/conf/config_ci.yml");

    static MetricAPIService initializeMetricAPIService() {
        ControllerAPIService controllerAPIService = initializeControllerAPIService();
        if (controllerAPIService != null) {
            logger.info("Attempting to setup Metric API Service");
            return controllerAPIService.getMetricAPIService();
        } else {
            logger.error("Failed to setup Metric API Service");
            return null;
        }
    }

    static CustomDashboardAPIService initializeCustomDashboardAPIService() {
        ControllerAPIService controllerAPIService = initializeControllerAPIService();
        if (controllerAPIService != null) {
            logger.info("Attempting to setup Dashboard API Service");
            return controllerAPIService.getCustomDashboardAPIService();
        } else {
            logger.error("Failed to setup Dashboard API Service");
            return null;
        }
    }


    private static ControllerAPIService initializeControllerAPIService() {
        Map<String, ?> config = YmlReader.readFromFileAsMap(configFile);
        config = ConfigProcessor.process(config);
        Map controllerInfoMap = (Map) config.get("controllerInfo");
        if (controllerInfoMap == null) {
            controllerInfoMap = Maps.newHashMap();
        }
        //this is for test purposes only
        controllerInfoMap.put("controllerHost","localhost");
        controllerInfoMap.put(ENCRYPTION_KEY, config.get(ENCRYPTION_KEY));
        try {
            ControllerInfo controllerInfo = ControllerInfoFactory.initialize(controllerInfoMap, installDir);
            logger.info("Initialized ControllerInfo");
            ControllerInfoValidator controllerInfoValidator = new ControllerInfoValidator(controllerInfo);
            if (controllerInfoValidator.isValidated()) {
                ControllerClient controllerClient = ControllerClientFactory.initialize(controllerInfo,
                        (Map<String, ?>) config.get("connection"), (Map<String, ?>) config.get("proxy"),
                        (String) config.get(ENCRYPTION_KEY));
                logger.debug("Initialized ControllerClient");
                return ControllerAPIServiceFactory.initialize(controllerInfo, controllerClient);
            }
        } catch (Exception ex) {
            logger.error("Failed to initialize the Controller API Service");
        }
        return null;
    }

}
