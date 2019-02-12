package com.appdynamics.extensions.kafka;
import com.appdynamics.extensions.controller.*;
import com.appdynamics.extensions.controller.apiservices.ControllerAPIService;
import com.appdynamics.extensions.controller.apiservices.ControllerAPIServiceFactory;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.util.JsonUtils;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Maps;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.appdynamics.extensions.Constants.ENCRYPTION_KEY;

/**
 * @author: {Vishaka Sekar} on {1/31/19}
 */
public class MetricCheckIT {

    private static final String USER_AGENT = "Mozilla/5.0";

    private CloseableHttpClient httpClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();


//    @Before
//    public void setup() {
//
//        CredentialsProvider provider = new BasicCredentialsProvider();
//        UsernamePasswordCredentials credentials
//                = new UsernamePasswordCredentials("admin@customer1", "admin");
//        provider.setCredentials(AuthScope.ANY, credentials);
//
//        httpClient = HttpClientBuilder.create()
//                .setDefaultCredentialsProvider(provider)
//                .build();
//    }
//
//    @After
//    public void tearDown() {
//        try {
//            httpClient.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testAgentAvailability() throws IOException{
//
//        UrlBuilder builder = UrlBuilder.builder();
//        builder.host("ec2-54-202-144-212.us-west-2.compute.amazonaws.com").port(8090).ssl(false).path("controller/rest/applications/Server%20&%20Infrastructure%20Monitoring/metric-data");
//        builder.query("metric-path", "Application%20Infrastructure%20Performance%7CRoot%7CHardware%20Resources%7CMachine%7CAvailability");
//        builder.query("time-range-type", "BEFORE_NOW");
//        builder.query("duration-in-mins", "60"); //TODO: 60 min is too long? can be shorter, like 15 min.
//        builder.query("output", "JSON");
//
//        CloseableHttpResponse httpResponse = sendGET(builder.build());
//
//        int statusCode = httpResponse.getStatusLine().getStatusCode();
//
//        Assert.assertEquals("Controller API is unreachable", 200, statusCode);
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(
//                httpResponse.getEntity().getContent()));
//
//        String inputLine;
//        StringBuffer response = new StringBuffer();
//
//        while ((inputLine = reader.readLine()) != null) {
//            response.append(inputLine);
//        }
//        reader.close();
//
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode jsonNode = mapper.readTree(response.toString());
//
//        String metricName = jsonNode.get(0).get("metricName").getTextValue();
//        int metricValue = jsonNode.get(0).get("metricValues").get(0).get("value").getIntValue();
//
//        Assert.assertEquals("Invalid metric name", "Hardware Resources|Machine|Availability", metricName);
//
//        Assert.assertEquals("Sim Agent is not up", 100, metricValue);
//
//
//    }
//
//    @Test
//    public void testAvgIdlePercentValueNotAlways100(){
//        //servers should not be 100% idle, if they are, we are not reporting that metric correctly
//    }
//
//    @Test
//    public void testIndividualTopicMetrics(){}
//
//    @Test
//    public void testMemoryLeak(){
//        //GC status metric, free memory metric , CPU Utilization
//    }
//
//    @Test
//    public void testTotalNumberOfMetricsReportedIsGreaterThanOne() throws IOException{
//
//        UrlBuilder builder = UrlBuilder.builder();
//        builder.host("ec2-54-202-144-212.us-west-2.compute.amazonaws.com").port(8090).ssl(false).path("controller/rest/applications/Server%20&%20Infrastructure%20Monitoring/metric-data");
//        builder.query("metric-path", "Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CMetrics%20Uploaded");
//        builder.query("time-range-type", "BEFORE_NOW");
//        builder.query("duration-in-mins", "60");
//        builder.query("output", "JSON");
//
//        CloseableHttpResponse httpResponse = sendGET(builder.build());
//
//        int statusCode = httpResponse.getStatusLine().getStatusCode();
//
//        Assert.assertEquals("Controller API is unreachable", 200, statusCode);
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(
//                httpResponse.getEntity().getContent()));
//
//        String inputLine;
//        StringBuffer response = new StringBuffer();
//
//        while ((inputLine = reader.readLine()) != null) {
//            response.append(inputLine);
//        }
//        reader.close();
//
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode jsonNode = mapper.readTree(response.toString());
//
//        String metricName = jsonNode.get(0).get("metricName").getTextValue();
//        int metricValue = jsonNode.get(0).get("metricValues").get(0).get("value").getIntValue();
//
//        Assert.assertEquals("Invalid metric name", "Custom Metrics|Kafka|Metrics Uploaded", metricName);
//
//        Assert.assertNotEquals("Agent is not reporting Kafka metrics", 1, metricValue);
//
//
//    }

    @Test
    public void testHeartBeatMetric() throws IOException {

        /*UrlBuilder builder = UrlBuilder.builder();
        builder.host("ec2-54-202-144-212.us-west-2.compute.amazonaws.com").port(8090).ssl(false).path("controller/rest/applications/Server%20&%20Infrastructure%20Monitoring/metric-data");
        builder.query("metric-path", "Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server%7Ckafka.server%7CHeartBeat");
        builder.query("time-range-type", "BEFORE_NOW");
        builder.query("duration-in-mins", "60");
        builder.query("output", "JSON");

        CloseableHttpResponse httpResponse = sendGET(builder.build());

        int statusCode = httpResponse.getStatusLine().getStatusCode();

        Assert.assertEquals("Invalid response code", 200, statusCode);

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                httpResponse.getEntity().getContent()));

        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();


        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.readTree(response.toString());

        String metricName = jsonNode.get(0).get("metricName").getTextValue();
        int metricValue = jsonNode.get(0).get("metricValues").get(0).get("value").getIntValue();


        Assert.assertEquals("Invalid metric name", "Custom Metrics|Kafka|Local Kafka Server|kafka.server|HeartBeat", metricName);

        Assert.assertEquals("Extension cannot connect to Kafka JMX port", 1, metricValue);*/

        ControllerInfo controllerInfo;
        ControllerClient controllerClient;
        ControllerAPIService controllerAPIService;
        MetricAPIService metricAPIService;
        File installDir = new File("/opt/appdynamics/monitors/KafkaMonitor/");


        File configFile = new File("/opt/appdynamics/monitors/KafkaMonitor/config.yml");
        Map<String, ?> config = YmlReader.readFromFileAsMap(configFile);

        Map controllerInfoMap = (Map) config.get("controllerInfo");
        if(controllerInfoMap == null) {
            controllerInfoMap = Maps.newHashMap();
        }
        controllerInfoMap.put(ENCRYPTION_KEY, config.get(ENCRYPTION_KEY));
        try {
            controllerInfo = ControllerInfoFactory.initialize(controllerInfoMap, installDir);
            ControllerInfoValidator controllerInfoValidator = new ControllerInfoValidator(controllerInfo);
            if (controllerInfoValidator.isValidated()) {
                controllerClient = ControllerClientFactory.initialize(controllerInfo,
                        (Map<String, ?>) config.get("connection"), (Map<String, ?>) config.get("proxy"),
                        (String) config.get(ENCRYPTION_KEY));
                controllerAPIService = ControllerAPIServiceFactory.initialize(controllerInfo, controllerClient);
                metricAPIService = controllerAPIService.getMetricAPIService();
                JsonNode jsonNode =  metricAPIService.getMetricData("", "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CLocal%20Kafka%20Server2%7Ckafka.server%7CHeartBeat&time-range-type=BEFORE_NOW&duration-in-mins=60&output=JSON");

                if (jsonNode != null) {
                    JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
                    int heartBeat = valueNode == null ? 0 : valueNode.get(0).asInt();
                    Assert.assertEquals("heartbeat is 0", heartBeat,1);
                }

            }

        } catch (Exception e) {
           e.printStackTrace();
        }


    }

    private CloseableHttpResponse sendGET(String url) throws IOException {

        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("User-Agent", USER_AGENT);
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);



        return httpResponse;


    }





}
