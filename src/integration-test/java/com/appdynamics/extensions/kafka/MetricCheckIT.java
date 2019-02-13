package com.appdynamics.extensions.kafka;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.util.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author: {Vishaka Sekar} on {1/31/19}
 */
public class MetricCheckIT {
    MetricAPIService metricAPIService;


    @Before
    public void setup() {

    File installDir = new File("src/integration-test/resources/conf/");
    File configFile = new File("src/integration-test/resources/conf/config_ci.yml");
// File installDir = new File("src/main/resources/conf/");
//    File configFile = new File("src/main/resources/conf/config.yml");
    metricAPIService = IntegrationTestUtils.setUpControllerClient(configFile,installDir);

    }

    @After
    public void tearDown() {
            //todo: shutdown client
    }

    @Test
    public void whenInstanceIsUpThenHeartBeatIs1ForServerWithSSLDisabled(){

    JsonNode jsonNode = null;
    if(metricAPIService != null) {
        jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server2%7Ckafka.server%7CHeartBeat&time-range-type=BEFORE_NOW&duration-in-mins=1440&output=JSON");
    }
    if (jsonNode != null) {
        JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
        int heartBeat = (valueNode == null) ? 0 : valueNode.get(0).asInt();
        Assert.assertEquals("heartbeat is 0", heartBeat,1);
    }

    else if(jsonNode == null ){
        System.out.println("JSON NODE NULL");

    }

    }

    @Test
    public void whenInstanceIsUpThenHeartBeatIs1ForServerWithSSLEnabled(){

        JsonNode jsonNode = null;
        if(metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server%7Ckafka.server%7CHeartBeat&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
            int heartBeat = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertEquals("heartbeat is 0", heartBeat,1);
        }

    }

    @Test
    public void whenMultiplierIsAppliedThenCheckMetricValue(){
        JsonNode jsonNode = null;
        if(metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server2%7Ckafka.server%7CHeartBeat&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
            int heartBeat = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertEquals("heartbeat is 0", heartBeat,1);
        }

    }






}
