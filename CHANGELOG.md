# AppDynamics Kafka Monitoring Extension 
## CHANGELOG

#### 2.0.4 - Jan 4, 2021
Updated to appd-exts-commons v2.2.4

#### 2.0.4 - July 5, 2020
Moved to appd-exts-commons v2.2.3
Added dashboard support and healthCheck module

#### 2.0.3 - Feb 22, 2019
Updated SIM metricPrefix in config.yml
#### 2.0.2 - Jan 23, 2019
Fixed null pointer in JMXConnection
#### 2.0.1 - Dec 18, 2018
Code fix so that metric properties are not applied multiple times
Refactoring code and test cases
Cleaner config.yml
removed multiple open connections, this will fix kafka monitor hitting the nproc limit.
#### 2.0.0 - Aug 8, 2018
Moved to 2.0 framework.
Added support for SSL
Added support for composite metrics