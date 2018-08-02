AppDynamics Monitoring Extension for use with Kafka
==============================
An AppDynamics extension to be used with a stand alone machine agent to provide metrics for Apache Kafka.


## Use Case ##
Apache Kafka® is a distributed, fault-tolerant streaming platform. It can be used to process streams of data in
real-time.

## Prerequisites ##

1.  This extension extracts the metrics from Kafka using the JMX protocol. Make sure you have configured JMX in Kafka
    To know more about JMX, please follow the below link
    http://docs.oracle.com/javase/6/docs/technotes/guides/management/agent.html

2.  Please ensure Kafka is up and running and is accessible from the the machine on which machine agent is installed.


## Troubleshooting steps ##
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.

1. Telnet into your Kafka server from the box where the extension is deployed.
       ```
          telnet <hostname> <port>

          <port> - It is the jmxremote.port specified.In the case of Kafka it is usually 9999
          <hostname> - IP address
       ```
  If telnet works, it confirm the access to the Kafka server.

2. Start jconsole:
   Jconsole comes as a utility with installed jdk. In your terminal, please type ```jconsole```
   In the remote connection text-box, please put in the service url :

   ```service:jmx:rmi:///jndi/rmi://<ip of the Kafka node>:9999/jmxrmi```

3. In the left pane of the jConsole, the list of mbeans exposed by Kafka is displayed.
   It is a good idea to match the mbean configuration in the config.yml against the jconsole.
   JMX is case sensitive so make sure the config matches exact.

## Metrics Provided ##

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller.
To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.
```
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

## Installation ##

1.  Run "mvn clean install" and find the KafkaMonitor.zip file in the "target" folder. You can also download the
    KafkaMonitor.zip from [AppDynamics Exchange][].
2.  Unzip as "KafkaMonitor" and copy the "KafkaMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`

# Configuration ##

Note : Please make sure to not use tab (\t) while editing yaml files.
You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Kafka instances by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/KafkaMonitor/`.
2. Below is the default config.yml which has metrics configured already
   For eg.

```
### ANY CHANGES TO THIS FILE DOES NOT REQUIRE A RESTART ###

#This will create this metric in all the tiers, under this path
metricPrefix: "Server|Component:<Tier-ID>|Custom Metrics|Kafka"

#This will create it in specific Tier/Component.
#Please make sure to replace <Tier-ID> with the appropriate one from your environment.
#To find the <Tier-ID> in your environment, please follow the screenshot
https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java


# List of Kafka Instances
# Please provide the service url (or) the host/port of each of the Kafka instances

servers:
    - serviceUrl: "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi"
      host: ""
      port: ""
      username: ""
      password: ""
      encryptedPassword: ""

# displayName is a REQUIRED field if you have more than 1 Kafka instance being
# otherwise please leave this field empty

      displayName: "Local Kafka Server"
# this field must be set to true if you want to access the metrics securely
#if this is set to true, the connection section below has more parameters that need to be configured.
      useSsl: true

#Provide the encryption key for the encrypted password
encryptionKey: ""


#Configure this section only if useSsl is set to true in any of the servers in the above section
connection:
    socketTimeout: 3000
    connectTimeout: 1000
    sslProtocol: "TLSv1.2"
    sslTrustStorePath: "/Users/vishaka.sekar/AppDynamics/client/kafka.client.truststore.jks"
    sslTrustStorePassword: "test1234"
    sslTrustStoreEncryptedPassword: ""

  #key store details for mutual auth on ssl
    sslKeyStorePath: "/Users/vishaka.sekar/AppDynamics/client/kafka.client.keystore.jks"
    sslKeyStorePassword: "test1234"
    sslKeyStoreEncryptedPassword: ""

# Set the following flag to true if you want to use the certs that are provided by default JDK
    useDefaultSslConnectionFactory: false

# Each Kafka server needs 1 thread each, so please configure this according to the number of servers you are monitoring
numberOfThreads: 10

# The configuration of different metrics from all mbeans exposed by Kafka server
mbeans:

# Each "objectName" is fully qualified path of the object of the Kafka server
# For example "kafka.server:type=BrokerTopicMetrics,*" will return all objects nested under ReplicaManager type
# Each of the entries in the "metrics" section is an attribute of the objectName under which it's listed

    - objectName: "kafka.server:type=BrokerTopicMetrics,*"
      metrics:
          - Count:
             alias: "Count"
             multiplier: ""
             delta: "false"
             aggregationType: "AVERAGE"
             timeRollUpType: "AVERAGE"
             clusterRollUpType: "INDIVIDUAL"

          - MeanRate:
             alias: "Mean Rate"
             multiplier: ""
             delta: "false"
             aggregationType: "AVERAGE"
             timeRollUpType: "AVERAGE"
             clusterRollUpType: "INDIVIDUAL"

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/KafkaMonitor/` directory. Below is the sample
   For Windows, make sure you enter the right path.
```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/KafkaMonitor/config.yml" />
          ....
     </task-arguments>
```


## Custom Dashboard ##
![](https://github.com/Appdynamics/kafka-monitoring-extension/blob/master/Kafka_CustomDashboard.png?raw=true)

## Enable JMX ##
To enable JMX Monitoring for Kafka broker, please follow below instructions:

Edit kafka-run-class.sh and modify KAFKA_JMX_OPTS variable like below (please replace <> with your Kafka Broker hostname)

```
KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=<kafka.broker.hostname >-Djava.net.preferIPv4Stack=true"
```
Add below line in kafka-server-start.sh

```
    export JMX_PORT=${JMX_PORT:-9999}
```

## Credentials Encryption
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) page to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following [document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130) for how to use the Extensions WorkBench

## Troubleshooting
Please follow the steps listed in the [extensions troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the troubleshooting-document to contact the support team.

## Support Tickets
If after going through the Troubleshooting Document you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.  

1. Stop the running machine agent .
2. Delete all existing logs under <MachineAgent>/logs .
3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug. 
   ```
   <logger name="com.singularity">
   <logger name="com.appdynamics">
     ```
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
5. Attach the zipped <MachineAgent>/conf/* directory here.
 6. Attach the zipped <MachineAgent>/monitors/<ExtensionMonitor> directory here .

For any support related questions, you can also contact help@appdynamics.com.

## Contributing
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/activemq-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.1.0       |
|Controller Compatibility  |4.4 or Later|
|Product Tested On         |Kafka 2.11. |
|Last Update               |07/18/2018 |
|List of Changes           |[Change log](https://github.com/Appdynamics/kafka-monitoring-extension/blob/master/Changelog.md) |

