AppDynamics Monitoring Extension for use with Kafka
===================================================
An AppDynamics extension to be used with a stand alone machine agent to provide metrics for Apache Kafka.


## Use Case ##
Apache Kafka® is a distributed, fault-tolerant streaming platform. It can be used to process streams of data in
real-time.

## Prerequisites ##

1.  This extension extracts the metrics from Kafka using the JMX protocol.
    Please ensure JMX is enabled <link to jmx section>
2.  Please ensure Kafka is up and running and is accessible from the the machine on which machine agent is installed.
3.  In order to use this extension, you do need a Standalone JAVA Machine Agent
    (https://docs.appdynamics.com/display/PRO44/Standalone+Machine+Agents)
    or SIM Agent (https://docs.appdynamics.com/display/PRO44/Server+Visibility).<br>
    For more details on downloading these products, please  visit https://download.appdynamics.com/.<br>
    The extension needs to be able to connect to the Kafka instance(s) in order to collect and send metrics.<br>
    To do this, you will have to either establish a remote connection in between the extension and Kafka,
    or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

## Enabling JMX
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.
1. Test connection to the port
   ```
   nc -v localhost 9999
   ```   
  
    If you get ```Connection to localhost port 9999 [tcp/distinct] succeeded!```,it confirms the access to the Kafka server. 
    <br>If not the JMX port needs to be enabled.
  
2.  <b>JMX configuration:</b>
    <br/>To enable JMX monitoring for Kafka broker, port 9999 has to be configured to allow monitoring on that port.
    <br>Edit the Kafka start-up script `<Kafka Installation Folder>/bin/kafka-server-start.sh` to include

       ```export JMX_PORT=${JMX_PORT:-9999}```
    Please note, that the Kafka server needs to be restarted once the JMX port is added.

3. <b>Start jConsole</b>:
   <br>jConsole comes as a utility with installed Java JDK.
   In your terminal, please type ```jconsole```.<br>
   In the remote connection text-box, please put in the service url :

   ```service:jmx:rmi:///jndi/rmi://<ip of the Kafka node>:9999/jmxrmi```

   In the left pane of the jConsole, the list of mbeans exposed by Kafka is displayed.
   It is a good idea to match the mbean configuration in the config.yml against the jconsole.
   JMX is case sensitive so make sure the config matches the names listed.
     
## Configuring Kafka Start-up scripts ##

   Edit `<Kafka Installation Folder>/bin/kafka-run-class.sh` and modify `KAFKA_JMX_OPTS` variable like below<br>
   
    KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
    
   Also, the changes to `kafka-run-class.sh` has to be made on all the Kafka servers that you wish to monitor.
   Please note, that any changes to  `kafka-run-class.sh` needs the Kafka server to be restarted for the changes to 
   take effect. 

## SSL and password authentication ###

If you need to monitor your Kafka servers securely via SSL , please enable the flags mentioned below.
<br/>Edit `<Kafka Installation Folder>/bin/kafka-run-class.sh` and modify `KAFKA_JMX_OPTS` variable like below<br>

    KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=true -Djavax.net.ssl.keyStore=/Absolute/path/to/keystore -Djavax.net.ssl.keyStorePassword=password -Dcom.sun.management.jmxremote.registry.ssl=false"
    
If you also need username/password authentication, please set the flag `-Dcom.sun.management.jmxremote.authenticate=true`
in the `KAFKA_JMX_OPTS` variable.

#### Keys ####

#### Password Settings ####
<br/>To know more on how to set the credentials, please see section below(username/password auth)





## Metrics Provided ##

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller.
To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.
```
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

## Installation ##

1.  Run "mvn clean install" and find the KafkaMonitor.zip file in the "target" folder. You can also download the
    KafkaMonitor.zip from [AppDynamics Exchange][https://www.appdynamics.com/community/exchange/].

2.  Unzip as "KafkaMonitor" and copy the "KafkaMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`
    Please place the extension in the "monitors" directory of your Machine Agent installation directory.
    Do not place the extension in the "extensions" directory of your Machine Agent installation directory.

## Configuration ##

Config.yml

Please copy all the contents of the config.yml file and go to http://www.yamllint.com/ . <br>
On reaching the website, paste the contents and press the “Go” button on the bottom left.<br>
If you get a valid output, that means your formatting is correct and you may move on to the next step.


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
   
   `<logger name="com.singularity">`
   `<logger name="com.appdynamics">`
    
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
5. Attach the zipped <MachineAgent>/conf/* directory here.
 6. Attach the zipped <MachineAgent>/monitors/<ExtensionMonitor> directory here .

For any support related questions, you can also contact help@appdynamics.com.

## Contributing
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/kafka-monitoring-extension).

## Version

Extension Version: 2.0.0
<br>Controller Compatibility: 4.0 or Later
<br>Tested On: Apache Kafka 2.11
<br>Operating System Tested On:  Mac OS
<br>Last updated On: Aug 08, 2018
<br>List of Changes to this extension [Change log](https://github.com/Appdynamics/kafka-monitoring-extension/blob/master/Changelog.md)
