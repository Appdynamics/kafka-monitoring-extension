AppDynamics Monitoring Extension for use with Kafka
==============================

An AppDynamics extension to be used with a stand alone machine agent to provide metrics for Apache Kafka


## Use Case ##

Kafka is a distributed, partitioned, replicated commit log service. It provides the functionality of a messaging system, but with a unique design.

## Prerequisites ##

This extension extracts the metrics from Kafka using the JMX protocol. Make sure you have configured JMX in Kafka

To know more about JMX, please follow the below link

 http://docs.oracle.com/javase/6/docs/technotes/guides/management/agent.html


## Troubleshooting steps ##
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.

1. Telnet into your Kafka server from the box where the extension is deployed.
       ```
          telnet <hostname> <port>

          <port> - It is the jmxremote.port specified.
          <hostname> - IP address
       ```


    If telnet works, it confirm the access to the Kafka server.


2. Start jconsole. Jconsole comes as a utility with installed jdk. After giving the correct host and port, check if Kafka
mbean shows up.

3. It is a good idea to match the mbean configuration in the config.yml against the jconsole. JMX is case sensitive so make
sure the config matches exact.

## Metrics Provided ##

In addition to the metrics exposed by Kafka, we also add a metric called "Metrics Collected" with a value 0 when an error occurs and 1 when the metrics collection is successful.

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.
```
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

## Installation ##

1. Run "mvn clean install" and find the KafkaMonitor.zip file in the "target" folder. You can also download the KafkaMonitor.zip from [AppDynamics Exchange][].
2. Unzip as "KafkaMonitor" and copy the "KafkaMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`

# Configuration ##

Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Kafka instances by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/KafkaMonitor/`.
2. Below is the default config.yml which has metrics configured already
   For eg.

```
### ANY CHANGES TO THIS FILE DOES NOT REQUIRE A RESTART ###

#This will create this metric in all the tiers, under this path
metricPrefix: "Server|Component:<Tier-ID>|Custom Metrics|Kafka"

#This will create it in specific Tier/Component.
#Please make sure to replace <Tier-ID> with the appropriate one from your environment.
#To find the <Tier-ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java


# List of Kafka Instances
servers:
  - host: "localhost"
    port: "9999"
    username: ""
    password: ""
    encryptedPassword: ""
    displayName: "Local Kafka Server"  #displayName is a REQUIRED field for level metrics.

encryptionKey: ""

connection:
  useSsl: true
  socketTimeout: 3000
  connectTimeout: 1000
  sslProtocols: ["TLSv1.2"]
  sslCertCheckEnabled: false
  sslVerifyHostname: false

  sslCipherSuites: ""
  sslTrustStorePath: ""
  sslTrustStorePassword: ""
  sslTrustStoreEncryptedPassword: ""
  sslKeyStorePath: ""
  sslKeyStorePassword: ""
  useDefaultSslConnectionFactory: true

# number of concurrent tasks.
# This doesn't need to be changed unless many instances are configured
numberOfThreads: 10

# The configuration of different metrics from various mbeans of Kafka server
# For most cases, the mbean configuration does not need to be changed.
mbeans:


  - objectName: "kafka.server:type=BrokerTopicMetrics,*"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

  - objectName: "kafka.server:type=DelayedFetchMetrics,*"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"
  - objectName: "kafka.server:type=KafkaRequestHandlerPool,*"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"
  - objectName: "kafka.server:type=ReplicaManager,name=IsrExpandsPerSec"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"
  - objectName: "kafka.server:type=ReplicaManager,name=IsrShrinksPerSec"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"
  - objectName: "kafka.server:type=SessionExpireListener,*"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"
  - objectName: "kafka.network:type=RequestMetrics,*"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

  - objectName: "kafka.controller:type=ControllerStats,*"
    metrics:
            Count:
                alias: "Count"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

            MeanRate:
                alias: "MeanRate"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"



  - objectName: "kafka.server:type=DelayedOperationPurgatory,*"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"


  - objectName: "kafka.server:type=KafkaServer,name=BrokerState"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

  - objectName: "kafka.server:type=ReplicaFetcherManager,*"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"


  - objectName: "kafka.server:type=ReplicaManager,name=LeaderCount"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

  - objectName: "kafka.server:type=ReplicaManager,name=PartitionCount"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

  - objectName: "kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

  - objectName: "kafka.network:type=Processor,*"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"

  - objectName: "kafka.network:type=Processor,*"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"


  - objectName: "kafka.network:type=RequestChannel,*"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"


  - objectName: "kafka.network:type=SocketServer,*"
    metrics:
            Value:
                alias: "Value"
                multiplier: ""
                delta: "false"
                aggregationType: "average"
                timeRollUpType: "average"
                clusterRollUpType: "individual"


#JVM Metrics
  - objectName: "java.lang:type=Memory"
    metrics:
          HeapMemoryUsage.committed: "Heap Memory Usage | Committed"
          HeapMemoryUsage.max: "Heap Memory Usage | Max"
          HeapMemoryUsage.used: "Heap Memory Usage | Used"
          NonHeapMemoryUsage.committed: "Heap Memory Usage | Committed"
          NonHeapMemoryUsage.used: "Heap Memory Usage | Used"
```

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

