Kafka Monitoring Extension for AppDynamics
===================================================

## Use Case ##
Apache Kafka® is a distributed, fault-tolerant streaming platform. It can be used to process streams of data in
real-time.The Kafka Monitoring extension can be used with a standalone machine agent to provide metrics for multiple 
Apache Kafka.

## Prerequisites ##
- Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
- Download and install [Apache Maven](https://maven.apache.org/) which is configured with `Java 8` to build the extension artifact from source. You can check the java version used in maven using command `mvn -v` or `mvn --version`. If your maven is using some other java version then please download java 8 for your platform and set JAVA_HOME parameter before starting maven.
- The extension also needs a [Kafka](https://kafka.apache.org/quickstart) server installed.
- The extension needs to be able to connect to Kafka in order to collect and send metrics. 
  To do this, you will have to either establish a remote connection in between the extension and the product, 
  or have an agent on the same machine running the product in order for the extension to collect and send the metrics.
  
## Installation ##
- Clone the "kafka-monitoring-extension" repo using `git clone <repoUrl>` command.
- Run 'mvn clean install' from "kafka-monitoring-extension". This will produce a KafkaMonitor-VERSION.zip in the target directory.
- Unzip the file KafkaMonitor-\[version\].zip into <b><MACHINE_AGENT_HOME>/monitors/</b>
- In the newly created directory <b>"KafkaMonitor"</b>, edit the config.yml to configure the parameters (See Configuration section below)
- Restart the Machine Agent

## Configuration ##

##### 1. Configuring ports
-  According to [Oracle's explanation](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8035404), JMX opens 3 different ports:
    - One is the JMX connector port(the one in config.yml)
    - One for the RMIRegistry
    - The third one is an ephemeral port is RMI registry of the local only server
-   We can explicitly configure the first two ports in the Kakfa start-up scripts to avoid it picking random ports.
-  Here port 9999 is used as JMX Connector port  9998 is used as the JMX/RMI port.
-  The third one, however, is an ephemeral port(that's how JMX works).
-  Test connection to the Kafka host and ports 9999 and 9998 from the machine where the extension is installed.

           For example, to test connection to the localhost on port 9999, use
            nc -v localhost 9999.
-   If the message ```Connection to localhost port 9999 [tcp/distinct] succeeded!```is displayed, it confirms the access to the Kafka server.

##### 2. Enabling JMX in Kafka
  - To enable JMX monitoring for Kafka broker, a JMX_PORT has to be configured to allow monitoring on that port.
    <br>Edit the Kafka start-up script `<Kafka Installation Folder>/bin/kafka-server-start.sh` to include:<br>
        `export JMX_PORT=${JMX_PORT:-9999}`<br/>
      This configures port 9999 as the JMX port of Kafka.
  - Please note that the Kafka server needs to be restarted once the JMX port is added.
  
##### 3. Configuring Kafka for non-SSL monitoring
   This section outlines the configuration of the Kafka start-up scripts if monitoring is <b>not</b> done over SSL.If SSL is being used please skip to [Setting up SSL in Kafka](#sslsettings).
   - To enable monitoring, some flags need to be set in the Kafka start-up scripts.
   Edit `<Kafka Installation Folder>/bin/kafka-run-class.sh` and modify `KAFKA_JMX_OPTS` variable like below<br>
     `KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.rmi.port=9998 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"`
   - Please also include `-Djava.rmi.server.hostname=your.host.ip` to the `KAFKA_JMX_OPTS` variable if you are monitoring Kafka from a remote machine.
   - Also, the changes to `kafka-run-class.sh` has to be made on all the Kafka servers that are being monitored.
   - Please note that any changes to  `kafka-run-class.sh` needs the Kafka server to be restarted for the changes to take effect.
##### <a name="sslsettings">4. Monitoring over SSL </a>
  If you need to monitor your Kafka servers securely via SSL, please follow the following steps:

##### 4.1. Configuring Kafka for monitoring over SSL  ####
   Edit `<Kafka Installation Folder>/bin/kafka-run-class.sh` and modify `KAFKA_JMX_OPTS` variable, as listed below:<br>
   ```
   KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.rmi.port=9998 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=true -Djavax.net.ssl.keyStore=/Absolute/path/to/keystore -Djavax.net.ssl.keyStorePassword=password -Dcom.sun.management.jmxremote.registry.ssl=false"
   ```
   - Please also include `-Djava.rmi.server.hostname=your.host.ip` to the `KAFKA_JMX_OPTS` variable if you are monitoring Kafka from a remote machine.
##### 4.2. Configuring the Extension to use SSL  ####
   - The Machine Agent and the extension also need to be configured to use SSL. Please add these flags to the Machine Agent start-up command:
   `-Djavax.net.ssl.trustStore=/path/to/client.truststore.jks -Djavax.net.ssl.trustStorePassword=yourpassword`
   
##### <a name = "passwordsettings"></a> 5. Password Settings 
If you need password authentication, the password needs to be set in the JVM of the Kafka server.
To know more on how to set the credentials, please see section `Using Password and Access Files` in [this link](https://docs.oracle.com/javase/8/docs/technotes/guides/management/agent.html).

##### 6. Config.yml 
Configure the Kafka monitoring extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/KafkaMonitor/`
  - Configure the "tier" under which the metrics need to be reported. This can be done by changing the value of `<Component-ID>` in
     `metricPrefix: "Server|Component:<Component-ID>|Custom Metrics|Kafka"`.<br/>Please refer this [link](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to find Component-ID of your tiers.
     For example,
     ```
     metricPrefix: "Server|Component:19|Custom Metrics|Kafka"
     ```
  - Configure the Kafka servers by specifying `<host,port>` of all Kafka servers.
     - Here, `host` is the IP address. 
       of the Kafka server to be monitored, and `port` is the JMX port of the Kafka server.
     - Please provide `username` & `password` (only if authentication enabled).
     - `encryptedPassword`(only if password encryption required).

  - Configure consumer lag calculations by configuring the `consumer_port` and the `lagTopics` regex string.

          The extension will get the topics and consumer groups from the kafka admin client interface,
          loop through and calculate and report any consumer lag metrics where the topic matches the regex. 
  
  - Configure the encyptionKey for encryptionPasswords(only if password encryption required).

          For example,
          #Encryption key for Encrypted password.
          encryptionKey: "axcdde43535hdhdgfiniyy576"

  - Configure the numberOfThreads according to the number of Kafka instances that are being monitored on one extension.Each server needs one thread.
        ```  For example,
                    If number Kafka servers that need to be monitored by one extension is 10, then number of threads is 10
                    numberOfThreads: 10
        ```
  - Configure the metrics section.<br/>
    For configuring the metrics, the following properties can be used:

       | Metric Property   |   Default value |         Possible values         |                                              Description                                                                                                |
       | :---------------- | :-------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------- |
       | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
       | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)    |
       | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)   |
       | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)|
       | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
       | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:0, DOWN:1  |
       | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |

     For example,
     `objectName:  "kafka.server:type=BrokerTopicMetrics, * ` will fetch metrics of all objects nested under `BrokerTopicMetrics`.

             - objectName: "kafka.server:type=BrokerTopicMetrics,*"
                  metrics:
                      - Count:
                         alias: "Count"
                         multiplier: ""
                         delta: false
                         aggregationType: "OBSERVATION"
                         timeRollUpType: "AVERAGE"
                         clusterRollUpType: "INDIVIDUAL"
            
                      - MeanRate:
                         alias: "Mean Rate"
                         multiplier: ""
                         delta: false
                         aggregationType: "AVERAGE"
                         timeRollUpType: "AVERAGE"
                         clusterRollUpType: "INDIVIDUAL"
    **All these metric properties are optional, and the default value shown in the table is applied to the metric(if a property has not been specified) by default.** If you need a metric from a specific object under an mBean, `objectName: kafka.server:type=ReplicaManager,name=IsrExpandsPerSec`   will return only those metrics corresponding to the `IsrExpandsPerSec` object.
##### 7. Validating config.yml:
- Please copy all the contents of the config.yml file and go to [Yaml Validator](https://jsonformatter.org/yaml-validator).
- On reaching the website, paste the contents and press the “Validate YAML” button.<br>
- If you get a valid output, that means your formatting is correct and you may move on to the next step.
## Metrics ##
- This extension collects metrics via JMX and can be configured to report any of the metrics that Kafka exposes. It provides metrics on Kafka server, controller and the network.
- In addition, it also provides the JVM metrics:<br/>
`HeapMemoryUsage.committed, HeapMemoryUsage.max, NonHeapMemoryUsage.committed, NonHeapMemoryUsage.max`
- There is also a `HeartBeat` metric under`kafka.server` which denotes whether the connection from the extension to the Kafka server was successful(1 = Successful, 0 = Unsuccessful).
## Credentials Encryption
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) page to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.
## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following
[document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130) for how to use the Extensions WorkBench
## Troubleshooting
Please follow the steps listed in the [extensions troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. 
These are a set of common issues that customers might have faced during the installation of the extension.
## Contributing
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/kafka-monitoring-extension).
## Version
| Name                        | Version                                                                                          | 
| :---------------------------|:-------------------------------------------------------------------------------------------------|
| Extension Version:          | 2.0.6                                                                                            |
| Tested On:                  | Apache Kafka 2.0.0                                                                               |
| Operating System Tested On: | Mac OS, Linux                                                                                    |
| Last updated On:            | 09/09/2024                                                                                       |
| List of changes to this extension| [Change log](https://github.com/Appdynamics/kafka-monitoring-extension/blob/master/CHANGELOG.md) 
  
**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.  
