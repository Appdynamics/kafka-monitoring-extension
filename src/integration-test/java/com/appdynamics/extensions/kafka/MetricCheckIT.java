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
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server2%7Ckafka.server%7CHeartBeat&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
    }
    if (jsonNode != null) {
        JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
        int heartBeat = (valueNode == null) ? 0 : valueNode.get(0).asInt();
        Assert.assertEquals("heartbeat is 0", heartBeat,1);
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
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server2%7Ckafka.server%7CKafkaRequestHandlerPool%7CRequestHandlerAvgIdlePercent%7COneMinuteRate&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
            int requestHandlerAvgPercent = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertTrue((requestHandlerAvgPercent > 90) && (requestHandlerAvgPercent <= 100));

        }

    }

    @Test
    public void checkTotalNumberOfMetricsReportedIsGreaterThan1(){
        JsonNode jsonNode = null;
        if(metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CMetrics%20Uploaded&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
            int totalNumberOfMetricsReported = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertTrue(totalNumberOfMetricsReported > 1);

        }

    }

    @Test
    public void whenAliasIsAppliedThenCheckMetricName(){

        JsonNode jsonNode = null;
        if(metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server2%7Ckafka.server%7CReplicaManager%7CUnderReplicatedPartitions%7CUnderReplicatedPartitions&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "metricName");
            String metricName = (valueNode == null) ? "" : valueNode.get(0).toString();
            int metricValue = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertEquals("Metric alias is invalid","\"Custom Metrics|Kafka|Local Kafka Server2|kafka.server|ReplicaManager|UnderReplicatedPartitions|Value\"",metricName);
            Assert.assertNotNull("Metric Value is  null in last 15min, maybe a stale metric ", metricValue);
        }

    }










}
