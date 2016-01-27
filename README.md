# Watson IoT MessageHub Connector

## Overview
The application will automatically create a topic in the specified MessageHub account in the format **``orgId``-events**.  On that topic the application will publish a new message for every event submitted by your Watson IoT devices.

- [IBM Watson IoT](https://internetofthings.ibmcloud.com)
- MessageHub

### Scalability
The application is scalabale both horizontally and vertically, you can either allocate more resources to the runtime or deploy more instances of the application as the number of events you need to process increases.  As you deploy new instances of the application Watson IoT will automatically distribute incoming events across your application instances.

### Message Format
Each event will be recorded as a single message in JSON format with the following elements:
- data - If the payload is parsable this will be a json representation of the data contained in the event
- payload - A base64 encoded representation of the raw event payload
- deviceId - The ``deviceId`` of the device that published the event
- typeId - The ``typeId`` of the device that published the event
- eventId - The ``eventId`` of the event
- format - The format of the event payload
- timestamp - An ISO8601 timestamp recording the time the event was **received** by the connector.

### Example Document
```json
{
  "data": {
    "mem": 9.1,
    "cpu": 1.8,
    "network": {
      "up": 0.02,
      "down": 0.68
    }
  },
  "payload": "eyJtZW0iOiA5LjEsICJjcHUiOiAxLjgsICJuZXR3b3JrIjogeyJkb3duIjogMC42OCwgInVwIjog MC4wMn19 ",
  "deviceId": "iot-test-01",
  "eventId": "psutil",
  "typeId": "vm",
  "format": "json",
  "timestamp": "2016-01-06T19:41:32.108595+00:00"
}
```

## Usage

### Standalone Execution
```bash
$ python connector-messagehub.py -c app.cfg -a myMessageHubApikey -u myMessageHubHost
Bottle v0.12.8 server starting up (using WSGIRefServer())...
Listening on http://localhost:8000/
Hit Ctrl-C to quit.
```


### Bluemix Deployment

#### Prerequisites
+ [GitHub client](https://github.com/)
+ [Cloud Foundry CLI](https://github.com/cloudfoundry/cli/releases)
+ [Bluemix account](https://bluemix.net/registration)

#### Deploy the application to Bluemix
When we initially deploy the application we do not want it to start up, because it's not yet bound to any services.
```bash
$ git clone https://github.com/ibm-iotf/connector-messagehub.git
$ cf login -u <username> -p <password> -o <org> -s <space>
$ cf push <app_name> --no-start
```

#### Optionally, create the required services
If you do not already have an instance of IoTF and MessageHub you will need to set one of each up.
```bash
$ cf create-service iotf-service iotf-service-free <iotf_instance_name>
$ cf create-service messagehub standard <messagehub_instance_name>
```

#### Bind the services to your application and start the service
Before we start the connector application we will bind an instance of MessageHub and an instance of IoTF to the application
```bash
$ cf bind-service <app_name> <iotf_instance_name>
$ cf bind-service <app_name> <messagehub_instance_name>
$ cf start <app_name>
```
