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

package com.ibm.iotf.connector.impl.publisher;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ibm.iotf.connector.Connector;
import com.ibm.iotf.connector.api.EventRepublisher;

public class MHubPublisher implements EventRepublisher {
	private static final Logger logger = Logger.getLogger(MHubPublisher.class);
	
	private KafkaProducer<byte[], byte[]> producer;
	private MessageHubEnvironment env;

	public MHubPublisher(MessageHubEnvironment env) throws IOException {
		this.env = env;
		this.producer = createProducer();
		logger.log(Level.INFO, "Kafka producer created.");
	}

	private KafkaProducer<byte[], byte[]> createProducer() throws IOException {		
		return new KafkaProducer<>(Connector.getClientConfiguration(env.getCredentials().getKafkaBrokersSasl()[0]));		
	}	

	@Override
	public void publishEvent(String eventKey, String eventJsonStr) {
		try {
			ProducerRecord<byte[], byte[]> sendRecord = new ProducerRecord<byte[], byte[]>(env.getTargetTopic(), eventKey.getBytes(), eventJsonStr.getBytes());
			Future<RecordMetadata> response = producer.send(sendRecord);		
			RecordMetadata result = response.get();
			logger.log(Level.DEBUG, "Event published to Message Hub: <topic: " + 
					result.topic() + ", partition: " + result.partition() + 
					", offset: " + result.offset() + ">");
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.ERROR, "Error while publishing event to Message Hub: " + e.getMessage(), e);
			logger.log(Level.ERROR, "Failed event: " + eventJsonStr);
		}		
	}

}
