# IoTF Cloudant Connector

## Overview
The application will automatically create a database in the specified Cloudant account in the format **``orgId``-events**.  Within that database the application will write a new document for every event submitted by your IoTF devices.  It also maintains a single design document named ``connector`` with some out of the box views you may find useful.  Any changes you make to this design document will be overwritten the next time the application starts, however you are free to create additional design documents.

The application does not provide an API to access the stored events, nor does it support exiry of older data.  Use the [Cloudant APIs](https://docs.cloudant.com/api.html) to retrieve and manage the stored events. 

- [Internet of Things Foundation](https://internetofthings.ibmcloud.com)
- [Cloudant](https://cloudant.com)


### Scalability
The application is scalabale both horizontally and vertically, you can either allocate more resources to the runtime or deploy more instances of the application as the number of events you need to process increases.  As you deploy new instances of the application IoTF will automatically distribute incoming events across your application instances.

### Document Format
Each event will be recorded as a single document in Cloudant with the following elements:
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
  _id: "040ff5c2c90d858d2bb02a6dbb4120b8",
  _rev: "1-6ae9e074b47d8cbf0cc2099d6cb22de9",
  data: {
    mem: 9.1,
    cpu: 1.8,
    network: {
      up: 0.02,
      down: 0.68
    }
  },
  payload: "eyJtZW0iOiA5LjEsICJjcHUiOiAxLjgsICJuZXR3b3JrIjogeyJkb3duIjogMC42OCwgInVwIjog MC4wMn19 ",
  deviceId: "iot-test-01",
  eventId: "psutil",
  typeId: "vm",
  format: "json",
  timestamp: "2016-01-06T19:41:32.108595+00:00"
}
```

## Usage

### Standalone Execution
```bash
$ python connector-cloudant.py -c app.cfg -u myCloudantUsername -p myCloudantPassword
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
$ git clone https://github.com/ibm-iotf/connector-cloudant.git
$ cf login -u <username> -p <password> -o <org> -s <space>
$ cf push <app_name> --no-start
```

#### Optionally, create the required services
If you do not already have an instance of IoTF and Cloudant you will need to set one of each up.
```bash
$ cf create-service iotf-service iotf-service-free <iotf_instance_name>
$ cf create-service cloudantNoSQLDB Shared <cloudant_instance_name>
```

#### Bind the services to your application and start the service
Before we start the connector application we will bind an instance of Cloudant and an instance of IoTF to the application
```bash
$ cf bind-service <app_name> <iotf_instance_name>
$ cf bind-service <app_name> <cloudant_instance_name>
$ cf start <app_name>
```
