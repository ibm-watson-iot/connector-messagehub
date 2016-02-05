/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Leo Davison - initial implementation
 */

package com.ibm.iotf.connector.impl.subscriber;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.google.gson.JsonObject;
import com.ibm.iotf.connector.api.EventSubscriber;

public class IoTFSubscriber extends EventSubscriber implements MqttCallback, Runnable {

	private static Logger logger = Logger.getLogger(IoTFSubscriber.class);

	private MqttAsyncClient appClient;
	private MqttConnectOptions connectOptions;
	private String topic;

	private static final Pattern DEVICE_EVENT_PATTERN = Pattern.compile("iot-2/type/(.+)/id/(.+)/evt/(.+)/fmt/(.+)");
	private static final DateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	private IoTFEnvironment env;
	private AtomicLong messagesReceived;

	public IoTFSubscriber(IoTFEnvironment env) {
		this.env = env;
		this.messagesReceived = new AtomicLong(0);
	}

	@Override
	public void run() {

		try {
			
			String serverURI = "ssl://" + env.getCredentials().getMqttHost() + ":" + env.getCredentials().getMqttPort();
			String username = env.getCredentials().getApiKey();
			String password = env.getCredentials().getApiToken();
			String clientID = "A:" + env.getCredentials().getOrg() + ":" + env.getAppId();
			
			topic = "iot-2/type/+/id/+/evt/+/fmt/+";

			appClient = new MqttAsyncClient(serverURI, clientID, null);
			connectOptions = new MqttConnectOptions();
			connectOptions.setUserName(username);
			connectOptions.setPassword(password.toCharArray());
			connectOptions.setCleanSession(true);

			SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(null, null, null);
			connectOptions.setSocketFactory(sslContext.getSocketFactory());
			appClient.setCallback(this);
			
			// connect retry loop
			boolean connected = false;
			do {
				
				connected = connect();
				
				if (!connected) {
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {}
				}
				
			} while (connected == false);

			// log out the number of events received every 5 seconds
			while (true) {

				try {
					Thread.sleep(5000);
				} catch (Exception e) {}

				logger.log(Level.INFO, "Total events received: " + messagesReceived.get());
			}

		} catch (Exception e) {
			logger.log(Level.FATAL, "Failure during client creation: " + e.getMessage(), e);
			System.exit(1);
		}
	}
	
	private boolean connect() {
		logger.log(Level.INFO, "Attemping to connect MQTT client.");
		try {
			appClient.connect(connectOptions).waitForCompletion();
			appClient.subscribe(topic, 0).waitForCompletion();
			logger.log(Level.INFO, "MQTT connection established.");
			return true;
		} catch (MqttSecurityException e) {		
			logger.log(Level.ERROR, "Failed to connect: " + e.getMessage(), e);
		} catch (MqttException e) {
			logger.log(Level.ERROR, "Failed to connect: " + e.getMessage(), e);
		}		
		return false;
	}	

	@Override
	public void connectionLost(Throwable arg0) {
		logger.log(Level.ERROR, "Connection Lost: " + arg0.getMessage(), arg0);
		logger.log(Level.INFO, "Reconnecting");
		
		boolean connected = false;
		do {
			
			connected = connect();
			
			if (!connected) {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {}
			}
			
		} while (connected == false);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// We aren't publishing, so this is not required.
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {
			Matcher matcher = DEVICE_EVENT_PATTERN.matcher(topic);

			if (matcher.matches()) {
				String type = matcher.group(1);
				String id = matcher.group(2);
				String event = matcher.group(3);
				String format = matcher.group(4);

				byte [] payload = message.getPayload();
				String payloadStr = new String(payload);

				logger.log(Level.DEBUG, "Event received:");
				logger.log(Level.DEBUG, "Device Type: " + type + "\n" + 
						"Device ID: " + id + "\n" + 
						"Event ID: " + event + "\n" + 
						"Format: " + format + "\n" + 
						"Payload: " + payloadStr);	

				messagesReceived.incrementAndGet();

				JsonObject eventJson = new JsonObject();
				String encodedPayload = DatatypeConverter.printBase64Binary(payload);
				
				logger.log(Level.DEBUG, "base64 encoded payload: " + encodedPayload);
				
				eventJson.addProperty("typeId", type);
				eventJson.addProperty("deviceId", id);
				eventJson.addProperty("eventId", event);
				eventJson.addProperty("format", format);
				eventJson.addProperty("timestamp", ISO8601_DATE_FORMAT.format(new Date()));
				eventJson.addProperty("payload", encodedPayload);

				String key = new StringBuilder(type).append(":").append(id).toString();

				this.handleEvent(key, eventJson.toString());
			}
		} catch (Throwable t) {
			logger.log(Level.ERROR, "Error during event processing/republish: " + t.getMessage(), t);			
		}		
	}
}
