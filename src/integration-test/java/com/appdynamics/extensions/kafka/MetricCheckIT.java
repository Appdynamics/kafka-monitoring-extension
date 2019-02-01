package com.appdynamics.extensions.kafka;
import com.appdynamics.extensions.http.UrlBuilder;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author: {Vishaka Sekar} on {1/31/19}
 */
public class MetricCheckIT {

    private static final String USER_AGENT = "Mozilla/5.0";

    private CloseableHttpClient httpClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Before
    public void setup() {

        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials("admin@customer1", "admin");
        provider.setCredentials(AuthScope.ANY, credentials);

        httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
    }

    @After
    public void tearDown() {
        try {
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAgentAvailability() throws IOException{

        UrlBuilder builder = UrlBuilder.builder();
        builder.host("ec2-54-202-144-212.us-west-2.compute.amazonaws.com").port(8090).ssl(false).path("controller/rest/applications/Server%20&%20Infrastructure%20Monitoring/metric-data");
        builder.query("metric-path", "Application%20Infrastructure%20Performance%7CRoot%7CHardware%20Resources%7CMachine%7CAvailability");
        builder.query("time-range-type", "BEFORE_NOW");
        builder.query("duration-in-mins", "60"); //TODO: 60 min is too long? can be shorter, like 15 min.
        builder.query("output", "JSON");

        CloseableHttpResponse httpResponse = sendGET(builder.build());

        int statusCode = httpResponse.getStatusLine().getStatusCode();

        Assert.assertEquals("Controller API is unreachable", 200, statusCode);

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

        Assert.assertEquals("Invalid metric name", "Hardware Resources|Machine|Availability", metricName);

        Assert.assertEquals("Sim Agent is not up", 100, metricValue);


    }

    @Test
    public void testAvgIdlePercentValueNotAlways100(){
        //servers should not be 100% idle, if they are, we are not reporting that metric correctly
    }

    @Test
    public void testIndividualTopicMetrics(){}

    @Test
    public void testMemoryLeak(){
        //GC status metric, free memory metric , CPU Utilization
    }

    @Test
    public void testTotalNumberOfMetricsReportedIsGreaterThanOne() throws IOException{

        UrlBuilder builder = UrlBuilder.builder();
        builder.host("ec2-54-202-144-212.us-west-2.compute.amazonaws.com").port(8090).ssl(false).path("controller/rest/applications/Server%20&%20Infrastructure%20Monitoring/metric-data");
        builder.query("metric-path", "Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CMetrics%20Uploaded");
        builder.query("time-range-type", "BEFORE_NOW");
        builder.query("duration-in-mins", "60");
        builder.query("output", "JSON");

        CloseableHttpResponse httpResponse = sendGET(builder.build());

        int statusCode = httpResponse.getStatusLine().getStatusCode();

        Assert.assertEquals("Controller API is unreachable", 200, statusCode);

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

        Assert.assertEquals("Invalid metric name", "Custom Metrics|Kafka|Metrics Uploaded", metricName);

        Assert.assertNotEquals("Agent is not reporting Kafka metrics", 1, metricValue);


    }

    @Test
    public void testJMXConnectionWithUserNamePasswordEnabled(){}
    //TODO: these have to be populated from config.yml

    @Test
    public void testJMXConnectionWithUserNameAndPasswordEncryptionEnabled(){}

    @Test
    public void testHeartBeatMetric() throws IOException {

        UrlBuilder builder = UrlBuilder.builder();
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

        Assert.assertEquals("Extension cannot connect to Kafka JMX port", 1, metricValue);

    }

    private CloseableHttpResponse sendGET(String url) throws IOException {

        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("User-Agent", USER_AGENT);
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        return httpResponse;
    }


}
