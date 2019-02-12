package com.appdynamics.extensions.kafka;
import com.appdynamics.extensions.http.UrlBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author: {Vishaka Sekar} on {2/11/19}
 */
public class IntegrationTestUtils {

    private static final String USER_AGENT = "Mozilla/5.0";

    public static UrlBuilder controllerQueryBuilder(String host,String metricPath, String timeRangeType, String durationInMin){

        UrlBuilder builder = UrlBuilder.builder();
        String path = "Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CKafka%7CMetrics%20Uploaded";
        builder.host(host).port(8090).ssl(false).path(path);
        builder.query("metric-path", metricPath);
        builder.query("time-range-type", timeRangeType);
        builder.query("duration-in-mins", durationInMin);
        builder.query("output", "JSON");
        return builder;
    }

    public static void validateMetricNameAndValue(HttpResponse httpResponse, String expectedMetricName, int expectedMetricValue)
            throws IOException{

        //TODO: add logging stmts


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

        Assert.assertEquals("Invalid metric name", expectedMetricName, metricName);
        Assert.assertEquals("Metric value mismatch ", expectedMetricValue, metricValue);

    }

    public static CloseableHttpClient createHttpClient(String username, String password){
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(AuthScope.ANY, credentials);

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();

        return httpClient;
    }


}
