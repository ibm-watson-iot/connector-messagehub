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

package com.ibm.iotf.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.iotf.connector.impl.publisher.MHubPublisher;
import com.ibm.iotf.connector.impl.publisher.MessageHubCredentials;
import com.ibm.iotf.connector.impl.publisher.MessageHubEnvironment;
import com.ibm.iotf.connector.impl.subscriber.IoTFEnvironment;
import com.ibm.iotf.connector.impl.subscriber.IoTFSubscriber;
import com.ibm.iotf.connector.utils.messagehub.CreateTopicParameters;
import com.ibm.iotf.connector.utils.messagehub.RESTRequest;

public class Connector {

	private static final String JAAS_CONFIG_PROPERTY = "java.security.auth.login.config";
	private static final Logger logger = Logger.getLogger(Connector.class);
	private static String userDir, resourceDir;
	private static boolean isBluemix;

	public static void main(String[] args) {
		userDir = System.getProperty("user.dir");
		resourceDir = null;
		isBluemix = false;

		IoTFSubscriber subscriber = null;
		MHubPublisher publisher = null;

		isBluemix = new File(userDir + File.separator + ".java-buildpack").exists();
		
		try {
			if (isBluemix) {
				logger.log(Level.INFO, "Running in Bluemix mode.");

				resourceDir = userDir + File.separator + "Connector-MessageHub-1.0" + File.separator + "bin" + File.separator + "resources";

				if(System.getProperty(JAAS_CONFIG_PROPERTY) == null) {
					System.setProperty(JAAS_CONFIG_PROPERTY, resourceDir + File.separator + "jaas.conf");
				}

				// Attempt to retrieve the environment configuration for the IoTF and Message Hub services
				IoTFEnvironment iotEnv = parseIoTFEnv();
				
				if (iotEnv == null) {
					logger.log(Level.FATAL, "Unable to retrieve the IoTF environment configuration.");
					System.exit(1);
				}
				
				MessageHubEnvironment messageHubEnv = parseProducerProps();
				
				if (messageHubEnv == null) {
					logger.log(Level.FATAL, "Unable to retrieve the Message Hub environment configuration.");
					System.exit(1);
				}
				
				// update the JAAS configuration with auth details from the environment
				// configuration we have just parsed
				updateJaasConfiguration(messageHubEnv.getCredentials());

				// create a single subscriber/producer
				subscriber = new IoTFSubscriber(iotEnv);
				publisher = new MHubPublisher(messageHubEnv);
				// configure the subscriber to hand off events to the publisher
				subscriber.setPublisher(publisher);

			} else {
				logger.log(Level.INFO, "Running in standalone mode - not currently supported.");
				System.exit(1);
			}

		} catch (Throwable t) {
			logger.log(Level.FATAL, "An error occurred while configuring and starting the environment: " + t.getMessage(), t);		
			System.exit(1);
		}

		logger.log(Level.INFO, "Starting the subscriber run loop.");
		// The subscriber run method + the thread(s) used by the IoT client libraries will keep
		// the application alive.
		subscriber.run();
	}


	public static <T> T parseEnv(String serviceName, Class<T> classType) {
		try {
			String vcap = System.getenv("VCAP_SERVICES");

			if (vcap == null) {
				logger.log(Level.FATAL, "VCAP_SERVICES env var not defined");
				return null;				
			}

			ObjectMapper mapper = new ObjectMapper();
			JsonNode vcapServicesJson = mapper.readValue(vcap, JsonNode.class);
			
			// only attempt to parse the config if the env has an entry for the requested service
			if (vcapServicesJson.has(serviceName)) {								
				T env = mapper.readValue(vcapServicesJson.get(serviceName).get(0).toString(), classType);
				return env;
			} else {
				logger.log(Level.FATAL, "Error parsing VCAP_SERVICES:  Could not find a service instance for '" + serviceName + "'.");
			}
		} catch (Exception e) {
			logger.log(Level.FATAL, "Error parsing service configuraton from VCAP_SERVICES for service '" + serviceName + "': " + e.getMessage(), e);
		}

		return null;
	}

	public static IoTFEnvironment parseIoTFEnv() {
		IoTFEnvironment env = parseEnv("iotf-service", IoTFEnvironment.class);
		if (env != null) {
			// this must be shared across all instances of the application in order
			// for the instances to participate in the same shared subscription to
			// device events.
			String appId = "messageHubConnector";
			env.setAppId(appId);		
		}
		return env;
	}

	public static MessageHubEnvironment parseProducerProps() {
		MessageHubEnvironment env = parseEnv("messagehub", MessageHubEnvironment.class);

		if (env != null) {
			String topic = System.getenv("MH_EVENT_TOPIC");

			if (topic != null) {
				env.setTargetTopic(topic);
				logger.log(Level.INFO, "Target Message Hub Topic: " + topic);

				String autoCreateTopic = System.getenv("MH_AUTO_CREATE_TOPIC");

				if (autoCreateTopic != null && autoCreateTopic.compareTo("1") == 0) {

					String partitionStr = System.getenv("MH_TOPIC_PARTITIONS");
					// default to 1 partition
					int partitionCount = 1;
					try {
						partitionCount = partitionStr != null ? Integer.parseInt(partitionStr) : 1;
					} catch(NumberFormatException e) {
						logger.log(Level.WARN, "MH_TOPIC_PARTITIONS not valid.  must be a number", e);
					}

					RESTRequest restApi = new RESTRequest(env.getCredentials().getKafkaRestUrl(), env.getCredentials().getApiKey());

					String res = restApi.post("admin/topics", new CreateTopicParameters(topic, partitionCount).toString(), new int[] { 422 });
					logger.log(Level.INFO, "Topic create POST cmd response: " + res);
				}
			} else {
				logger.log(Level.FATAL, "No target Message Hub topic specified.  Please create a topic and set the 'MH_EVENT_TOPIC' app env var with the name.");
				return null;
			}					
		}

		return env;		
	}

	public static final Properties getClientConfiguration(String broker) throws IOException {
		Properties props = new Properties();
		InputStream propsStream;
		String fileName = "producer.properties";
		String propertiesPath = resourceDir + File.separator + fileName;
		
		try {			
			propsStream = new FileInputStream(propertiesPath);
			props.load(propsStream);
			propsStream.close();
		} catch (IOException e) {
			logger.log(Level.FATAL, "Could not load Message Hub producer properties from file: " + propertiesPath);
			throw e;
		}

		props.put("bootstrap.servers", broker);

		// if we're running in bluemix, we need to update the location of the Java truststore
		if(isBluemix) {
			props.put("ssl.truststore.location", userDir + "/.java-buildpack/open_jdk_jre/lib/security/cacerts");
		}

		return props;
	}

	private static void updateJaasConfiguration(MessageHubCredentials credentials) throws IOException {
		String templatePath = resourceDir + File.separator + "templates" + File.separator + "jaas.conf.template";
		String path = resourceDir + File.separator + "jaas.conf";
		OutputStream jaasStream = null;

		logger.log(Level.INFO, "Updating JAAS configuration");

		try {
			String templateContents = new String(Files.readAllBytes(Paths.get(templatePath)));
			jaasStream = new FileOutputStream(path, false);

			// Replace username and password in template and write
			// to jaas.conf in resources directory.
			String fileContents = templateContents
					.replace("$USERNAME", credentials.getUser())
					.replace("$PASSWORD", credentials.getPassword());

			jaasStream.write(fileContents.getBytes(Charset.forName("UTF-8")));
		} catch (final IOException e) {
			logger.log(Level.FATAL, "Error while reading/updating JAAS configuration file: " + e.getMessage(), e);			
			throw e;
		} finally {
			if(jaasStream != null) {
				try {
					jaasStream.close();
				} catch(final Exception e) {
					logger.log(Level.WARN, "Error closing JAAS config file: " + e.getMessage(), e);					
				}
			}
		}
		
	}
}
