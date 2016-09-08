kafka-monitoring-extension
==============================

An AppDynamics extension to be used with a stand alone Java machine agent to provide metrics for Apache Kafka


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


2. Start jconsole. Jconsole comes as a utility with installed jdk. After giving the correct host and port , check if Kafka
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
   # Kafka particulars
   server:
       #Kafka host
       host: "localhost"
       #Kafka JMX port
       port: 9999
       username: ""
       password: ""
       domains:
           -   name: "kafka.server"
               excludeObjects: ["BytesInPerSec"]
           -   name: "kafka.cluster"
               excludeObjects: []
           -   name: "kafka.controller"
               excludeObjects: []
           -   name: "kafka.network"
               excludeObjects: []
           -   name: "kafka.log"
               excludeObjects: []
           -   name: "kafka.consumer"
               excludeObjects: []

   #prefix used to show up metrics in AppDynamics
   metricPathPrefix:  "Custom Metrics|Kafka|"

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


## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [AppDynamics Exchange][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 1.0.0
**Controller Compatibility:** 3.7+
**Kafka Versions Tested On:** 2.11-0.9.0.0

[Github]: https://github.com/Appdynamics/kafka-monitoring-extension
[AppDynamics Exchange]: http://community.appdynamics.com/t5/AppDynamics-eXchange/idb-p/extensions
[AppDynamics Center of Excellence]: mailto:help@appdynamics.com
