Kafka Monitoring Extension for AppDynamics
===================================================

## Use Case ##
Apache Kafka® is a distributed, fault-tolerant streaming platform. It can be used to process streams of data in
real-time.The Kafka Monitoring extension can be used with a stand alone machine agent to provide metrics for multiple 
Apache Kafka.
## Prerequisites ##
- In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Standalone+Machine+Agents).
or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).For more details on downloading these products, please  visit [Downloads](https://download.appdynamics.com/).<br>
- The extension also needs a [Kafka](https://kafka.apache.org/quickstart) server installed.
- The extension needs to be able to connect to Kafka in order to collect and send metrics. 
  To do this, you will have to either establish a remote connection in between the extension and the product, 
  or have an agent on the same machine running the product in order for the extension to collect and send the metrics.
## Installation ##
- To build from source, clone this repository and run 'mvn clean install'. This will produce a KafkaMonitor-VERSION.zip in the target directory Alternatively, download the latest release archive from [GitHub](#https://github.com/Appdynamics/kafka-monitoring-extension)
- Unzip the file KafkaMonitor-\[version\].zip into <b><MACHINE_AGENT_HOME>/monitors/</b>
- In the newly created directory <b>"KafkaMonitor"</b>, edit the config.yml to configure the parameters (See Configuration section below)
- Restart the Machine Agent
- In the AppDynamics Metric Browser, look for: In the AppDynamics Metric Browser, look for: Application Infrastructure Performance|\<Tier\>|Custom Metrics|Log Monitor. If SIM is enabled, look for the metric browser under the Servers tab.
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
##### 2. Enabling JMX
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
   - Also, the changes to `kafka-run-class.sh` has to be made on all the Kafka servers that are being monitored.
   - Please note that any changes to  `kafka-run-class.sh` needs the Kafka server to be restarted for the changes to take effect.
##### <a name="sslsettings">4. Monitoring over SSL </a>
  If you need to monitor your Kafka servers securely via SSL, please follow the following steps:
##### 4.1. Generating SSL Keys
  -  Providing a Keystore and Truststore is mandatory for using SSL. The Keystore is used by the Kafka server, the Truststore is used by the Kafka Monitoring Extension to trust the server.
  -  The extension supports a custom Truststore, and if no Truststore is specified, the extension defaults to the Machine Agent Truststore at `<Machine_Agent_Home>/conf/cacerts.jks`.
  -  <b>You can create your Truststore or choose to use the Machine Agent Truststore at `<MachineAgentHome>/conf/cacerts.jks`.</b>
  -  Keytool is a utility that comes with the JDK. Please use the following commands to generate a keystore, and import the certificates into the Truststore.
  -  To use the custom Truststore, please follow steps 1, 2 and 3a listed below.
  -  To to use the Machine Agent Truststore `cacerts.jks`, please follow the steps 1, 2 and 3b listed below to import the certs into `cacerts.jks`.

            #Step #1
            keytool -keystore kafka.server.keystore.jks -alias localhost -validity 365 -genkey
            
            #Step #2 
            openssl req -new -x509 -keyout ca-key -out ca-cert -days 365
            
            #Step #3a: if you are creating your own truststore 
            keytool -keystore kafka.client.truststore.jks -alias CARoot -import -file ca-cert
            
            #Step #3b: or if you are using Machine Agent truststore 
            keytool -keystore /path/to/MachineAgentHome/conf/cacerts.jks -alias CARoot -import -file ca-cert
   - Additional info about creating SSL keys is listed [here](https://docs.confluent.io/current/tutorials/security_tutorial.html#creating-ssl-keys-and-certificates).
##### 4.2. Configuring Kafka for monitoring over SSL  ####
   Edit `<Kafka Installation Folder>/bin/kafka-run-class.sh` and modify `KAFKA_JMX_OPTS` variable, as listed below:<br>
   ```
   KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.rmi.port=9998 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=true -Djavax.net.ssl.keyStore=/Absolute/path/to/keystore -Djavax.net.ssl.keyStorePassword=password -Dcom.sun.management.jmxremote.registry.ssl=false"
   ```
##### 4.3. Configuring the Extension to use SSL  ####
   - The extension also needs to be configured to use SSL. In the config.yml of the Kafka Extension, uncomment the `connection` section.<br/>
      ```
              connection:
                socketTimeout: 3000
                connectTimeout: 1000
                sslProtocol: "TLSv1.2"
                sslTrustStorePath: "/path/to/truststore/client/kafka.client.truststore.jks" #defaults to <MA home>conf/cacerts.jks
                sslTrustStorePassword: "test1234" # defaults to empty
                sslTrustStoreEncryptedPassword: ""
      ```
   - <b> Please note that any changes to the </b> `connection`<b> section of the config.yml, needs the Machine Agent to
      be restarted for the changes to take effect.</b>          
  - If you  need username/password authentication, please set the flag<br/>`-Dcom.sun.management.jmxremote.authenticate=true`
     in the `KAFKA_JMX_OPTS` variable.Please refer to [Password Settings](#passwordsettings) for further steps.
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
  - Configure the Kafka servers by specifying <b>either</b> `serviceUrl` or `<host,port>` of all Kafka servers.
     - Here, `host` is the IP address. 
       of the Kafka server to be monitored, and `port` is the JMX port of the Kafka server.
     - Please provide `username` & `password` (only if authentication enabled).
     - `encryptedPassword`(only if password encryption required).
     - If SSL is being used to securely monitor your Kafka servers, please set `useSsl` as `true`.
          For example,
          ```
          - serviceUrl: "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi" #provide service URL or the <host,port> pair
                host: ""
                port: ""
                username: "monitorRole"
                password: "QED"
                encryptedPassword: ""
                displayName: "Local Kafka Server"
                useSsl: true # set to true if you're using SSL for this server
          ```
  - Configure the encyptionKey for encryptionPasswords(only if password encryption required).

          For example,
          #Encryption key for Encrypted password.
          encryptionKey: "axcdde43535hdhdgfiniyy576"

  - Configure the connection section only if you are using monitoring over SSL for <b>ANY</b> of Kafka server(s).
     - Please remove this section if SSL is not required to connect to any of your servers.
     - If you are using the Machine Agent Truststore, please leave the `sslTrustStorePath` as `""`.
       ```
       connection:
         socketTimeout: 3000
         connectTimeout: 1000
         sslProtocol: "TLSv1.2"
         sslTrustStorePath: "/path/to/truststore/client/kafka.client.truststore.jks" #defaults to <MA home>conf/cacerts.jks
         sslTrustStorePassword: "test1234" # defaults to empty
         sslTrustStoreEncryptedPassword: ""
       ```

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
       | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
       | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
       | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
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
- Please copy all the contents of the config.yml file and go to [YamlLint](http://www.yamllint.com/).
- On reaching the website, paste the contents and press the “Go” button on the bottom left.<br>
- If you get a valid output, that means your formatting is correct and you may move on to the next step.
##### 8. Metrics
- This extension collects metrics via JMX and can be configured to report any of the metrics that Kafka exposes. It provides metrics on Kafka server, controller and the network.
- In addition, it also provides the JVM metrics:<br/>
`HeapMemoryUsage.committed, HeapMemoryUsage.max, NonHeapMemoryUsage.committed, NonHeapMemoryUsage.max`
- There is also a `HeartBeat` metric under`kafka.server` which denotes whether the connection from the extension to the Kafka server was successful(1 = Successful, 0 = Unsuccessful).
- By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller.
  To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
## Credentials Encryption
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) page to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.
## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following
[document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130) for how to use the Extensions WorkBench
## Troubleshooting
Please follow the steps listed in the [extensions troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. 
These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the troubleshooting-document to contact the support team.
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
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/kafka-monitoring-extension).
## Version
| Name                        |  Version                    | 
| :---------------------------| :---------------------------|
| Extension Version:          | 2.0.0                       | 
| Controller Compatibility:   | 4.0 or Later                |
| Tested On:                  | Apache Kafka 2.11           |
| Operating System Tested On: | Mac OS                      |
| Last updated On:            | Aug 27, 2018                |
| List of changes to this extension| [Change log](https://github.com/Appdynamics/kafka-monitoring-extension/blob/master/Changelog.md) 
