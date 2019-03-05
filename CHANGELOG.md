# AppDynamics Kafka Monitoring Extension CHANGELOG

## 2.0.3 - Feb 22, 2019
1. Updated SIM metricPrefix in config.yml

## 2.0.2 - Jan 23, 2019
1. Fixed null pointer in JMXConnection

## 2.0.1 - Dec 18, 2018
1. Code fix so that metric properties are not applied multiple times
2. Refactoring code and test cases
3. Cleaner config.yml
4. Removed multiple open connections, this will fix kafka monitor
hitting the `nproc` limit.

## 2.0.0 - Aug 8,  2018
1. Moved to 2.0 framework.
2. Added support for SSL
3. Added support for composite metrics 




