import os
import json
import ibmiotf.application
import iso8601
import base64
from bottle import Bottle, template

import requests
from requests_futures.sessions import FuturesSession

import urllib
import argparse
import logging
from logging.handlers import RotatingFileHandler

class Server():

	def __init__(self, args):
		# Setup logging - Generate a default rotating file log handler and stream handler
		logFileName = 'connector-messagehub.log'
		fhFormatter = logging.Formatter('%(asctime)-25s %(name)-30s ' + ' %(levelname)-7s %(message)s')
		rfh = RotatingFileHandler(logFileName, mode='a', maxBytes=26214400 , backupCount=2, encoding=None, delay=True)
		rfh.setFormatter(fhFormatter)
		
		self.logger = logging.getLogger("server")
		self.logger.addHandler(rfh)
		self.logger.setLevel(logging.DEBUG)
		
		
		self.port = int(os.getenv('VCAP_APP_PORT', '8000'))
		self.host = str(os.getenv('VCAP_APP_HOST', 'localhost'))

		if args.bluemix == True:
			# Bluemix VCAP lookups
			application = json.loads(os.getenv('VCAP_APPLICATION'))
			service = json.loads(os.getenv('VCAP_SERVICES'))
			
			# IoTF
			self.options = ibmiotf.application.ParseConfigFromBluemixVCAP()
			
			# Cloudant
			self.messagehubApikey = service['messagehub'][0]['credentials']['api_key']
			self.messagehubUrl = service['messagehub'][0]['credentials']['kafka_rest_url']
		else:
			self.options = ibmiotf.application.ParseConfigFile(args.config)
			self.messagehubApikey = args.messagehubApikey
			self.messagehubUrl = args.messagehubUrl
		
		
		# Bottle
		self._app = Bottle()
		self._route()
		
		# Init MessageHub Topic & requests session
		self._createTopic()
		self.session = FuturesSession()
		
		# Init IOTF client
		self.client = ibmiotf.application.Client(self.options, logHandlers=[rfh])
	
	
	def _route(self):
		self._app.route('/', method="GET", callback=self._status)
	
	
	def myEventCallback(self, evt):
		#self.logger.info("%-33s%-30s%s" % (evt.timestamp.isoformat(), evt.device, evt.event + ": " + json.dumps(evt.data)))
		#self.logger.info(evt.data)
		
		headers = {
			"Content-Type": "application/json", 
			"X-Auth-Token": self.messagehubApikey
		}
		
		message = {
			'typeId': evt.deviceType,
			'deviceId': evt.deviceId,
			'eventId': evt.event,
			'timestamp': evt.timestamp.isoformat(),
			'data': evt.data,
			'format': evt.format,
			'payload': base64.encodestring(evt.payload).decode('ascii')
		}
		
		payload = {
			'records': [{"value": json.dumps(message)}]
		}
		
		future = self.session.post(self.messagehubUrl + "/topics/events", data=json.dumps(payload), headers=headers)
		future.add_done_callback(self._eventRecordedCallback)
	
	
	def _eventRecordedCallback(self, future):
		response = future.result()
		if response.status_code not in [200]:
			self.logger.info("%s - %s: %s" % (response.url, response.status_code, response.text))
		
	
	def start(self):
		self.client.connect()
		self.client.deviceEventCallback = self.myEventCallback
		self.client.subscribeToDeviceEvents()
		self.logger.info("Serving at %s:%s" % (self.host, self.port))
		self._app.run(host=self.host, port=self.port)
	
	def stop(self):
		self.client.disconnect()
		
	def _status(self):
		return template('status', env_options=os.environ)


	# =============================================================================
	# MessageHub methods
	# =============================================================================
	def _createTopic(self):
		headers = {
			"Content-Type": "application/json", 
			"X-Auth-Token": self.messagehubApikey
		}
		payload = {"name": "events"}
		
		r = requests.post(self.messagehubUrl + "/admin/topics", data=json.dumps(payload), headers=headers)
		print(r.text)




# Initialize the properties we need
parser = argparse.ArgumentParser()
parser.add_argument('-b', '--bluemix', required=False, action='store_true')
parser.add_argument('-c', '--config', required=False)
parser.add_argument('-a', '--messagehubApikey', required=False)
parser.add_argument('-u', '--messagehubUrl', required=False)

args, unknown = parser.parse_known_args()

server = Server(args)
server.start()