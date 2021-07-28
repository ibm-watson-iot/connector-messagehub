# Watson IoT MessageHub Connector

## Overview
The application subscribes to all events for all devices in a Watson IoT organization, and re-publishes the events to a topic in a Message Hub account.  The Message Hub topic that is published to is defined in the **manifest.yml** file:  
```
MH_EVENT_TOPIC: event_topic  
MH_AUTO_CREATE_TOPIC: 1  
MH_TOPIC_PARTITIONS: 2  
```  
If `MH_EVENT_TOPIC` does not exist in the Message Hub account, and `MH_AUTO_CREATE_TOPIC=1`, then the topic will be created on your behalf with the number of partitions specified by `MH_TOPIC_PARTITIONS`.  If the topic already exists, then it will remain unchanged (even if `MH_TOPIC_PARTITIONS` is modified).

- [IBM Watson IoT](https://internetofthings.ibmcloud.com)
- [IBM Message Hub](https://developer.ibm.com/messaging/message-hub/)

## Product Withdrawal Notice
Per the September 8, 2020 [announcement](https://www-01.ibm.com/common/ssi/cgi-bin/ssialias?subtype=ca&infotype=an&appname=iSource&supplier=897&letternum=ENUS920-136#rprodnx) IBM Watson IoT Platform (5900-A0N) has been withdrawn from marketing effective **December 9, 2020**.  As a result, updates to this project will be limited.

### Scalability
The application is scalabale both horizontally and vertically, you can either allocate more resources to the runtime or deploy more instances of the application as the number of events you need to process increases.  As you deploy new instances of the application Watson IoT will automatically distribute incoming events across your application instances.  

The number of instances of the application that are deployed can be controlled by modifying the `instances: 1` entry of the **manifest.yml** file, prior to pushing to Bluemix.

### Message Format
Each event will be recorded as a single message in JSON format with the following elements:  
- typeId - The ``typeId`` of the device that published the event  
- deviceId - The ``deviceId`` of the device that published the event  
- eventId - The ``eventId`` of the event  
- format - The format of the event payload  
- timestamp - An ISO8601 timestamp recording the time the event was **received** by the connector.  
- payload - A base64 encoded representation of the raw event payload  

### Example Document
```json
{
  "typeId": "vm",
  "deviceId": "iot-test-01",
  "eventId": "psutil",  
  "format": "json",
  "timestamp": "2016-01-06T19:41:32.108595+00:00",
  "payload": "eyJtZW0iOiA5LjEsICJjcHUiOiAxLjgsICJuZXR3b3JrIjogeyJkb3duIjogMC42OCwgInVwIjog MC4wMn19 "
}
```

## Usage

### Bluemix Deployment

#### Prerequisites
+ [GitHub client](https://github.com/)
+ [Cloud Foundry CLI](https://github.com/cloudfoundry/cli/releases)
+ [Bluemix account](https://bluemix.net/registration)

#### Build the application
Before we can push the application to Bluemix, we have to build the application bundle.  The appropriate version of gradle will be downloaded by the `gradlew` command.
```bash
$ git clone https://github.com/ibm-iotf/connector-messagehub.git
$ cd connector-messagehub/Connector-MessageHub
$ ./gradlew clean && ./gradlew build
```

#### Login to Bluemix using the `cf` tool
Before you can create services and push the application to Bluemix, you must login to your Bluemix account (example below connects to Bluemix us-south region).
```bash
$ cf login -a https://api.ng.bluemix.net -u <username> -p <password> -o <org> -s <space>
```

#### Optionally, create the required services
If you do not already have an instance of the Watson IoTF and Message Hub services you will need to set one of each up. Otherwise, you can skip these commands, but you must update the `services:` entry of the **manifest.yml** file to specify the appropriate names of your Watson IoT and Message Hub services, or the application will not be able to bind to these services correctly.
```bash
$ cf create-service iotf-service iotf-service-free iotf
$ cf create-service messagehub standard messagehub
```

#### Deploy the application to Bluemix
Now that we have built the application bundle and created the required services, we can deploy it to Bluemix.
```bash
$ cf push
```
