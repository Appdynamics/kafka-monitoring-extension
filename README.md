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
metricPrefix: Custom Metrics|Kafka

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
#metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|Kafka

# List of Kafka Instances
instances:
  - host: "localhost"
    port: 9999
    username:
    password:
    #encryptedPassword:
    #encryptionKey:
    displayName: "Local Kafka Server"  #displayName is a REQUIRED field for level metrics.


# number of concurrent tasks.
# This doesn't need to be changed unless many instances are configured
numberOfThreads: 10


# The configuration of different metrics from various mbeans of Kafka server
# For most cases, the mbean configuration does not need to be changed.
mbeans:

#All MBeans which have attributes Count and MeanRate
  - mbeanFullPath: ["kafka.server:type=BrokerTopicMetrics,*",
                    "kafka.server:type=DelayedFetchMetrics,*",
                    "kafka.server:type=KafkaRequestHandlerPool,*",
                    "kafka.server:type=ReplicaManager,name=IsrExpandsPerSec",
                    "kafka.server:type=ReplicaManager,name=IsrShrinksPerSec",
                    "kafka.server:type=SessionExpireListener,*",
                    "kafka.network:type=RequestMetrics,*",
                    "kafka.controller:type=ControllerStats,*"
                  ]
    metrics:
       include:
          - Count: "Count"
          - MeanRate: "MeanRate"

#All MBeans which have attributes Value
  - mbeanFullPath: ["kafka.server:type=DelayedOperationPurgatory,*",
                    "kafka.server:type=KafkaServer,name=BrokerState",
                    "kafka.server:type=ReplicaFetcherManager,*",
                    "kafka.server:type=ReplicaManager,name=LeaderCount",
                    "kafka.server:type=ReplicaManager,name=PartitionCount",
                    "kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions",
                    "kafka.network:type=Processor,*",
                    "kafka.network:type=RequestChannel,*",
                    "kafka.network:type=SocketServer,*"
                   ]
    metrics:
       include:
          - Value: "Value"
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
    
## Password Encryption Support
To avoid setting the clear text password in the config.yaml, please follow the process below to encrypt the password

1. Download the util jar to encrypt the password from [https://github.com/Appdynamics/maven-repo/blob/master/releases/com/appdynamics/appd-exts-commons/1.1.2/appd-exts-commons-1.1.2.jar](https://github.com/Appdynamics/maven-repo/blob/master/releases/com/appdynamics/appd-exts-commons/1.1.2/appd-exts-commons-1.1.2.jar) and navigate to the downloaded directory
2. Encrypt password from the commandline
`java -cp appd-exts-commons-1.1.2.jar com.appdynamics.extensions.crypto.Encryptor encryptionKey myPassword`
3. Specify the passwordEncrypted and encryptionKey in config.yaml

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

## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [AppDynamics Exchange][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 1.0.0
**Controller Compatibility:** 3.7+
**Kafka Versions Tested On:** 2.11-0.9.0.0 and 2.11-0.10.1.0

[Github]: https://github.com/Appdynamics/kafka-monitoring-extension
[AppDynamics Exchange]: https://www.appdynamics.com/community/exchange/kafka-monitoring-extension/
[AppDynamics Center of Excellence]: mailto:help@appdynamics.com
