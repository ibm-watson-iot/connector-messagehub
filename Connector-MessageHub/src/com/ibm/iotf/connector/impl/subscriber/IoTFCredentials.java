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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class IoTFCredentials {
	private String mqttHost, mqttPort, org, apiKey, apiToken;

	@JsonProperty("mqtt_host")
	public String getMqttHost() {
		return mqttHost;
	}

	@JsonProperty("mqtt_host")
	public void setMqttHost(String mqttHost) {
		this.mqttHost = mqttHost;
	}

	@JsonProperty("mqtt_s_port")
	public String getMqttPort() {
		return mqttPort;
	}

	@JsonProperty("mqtt_s_port")
	public void setMqttPort(String mqttPort) {
		this.mqttPort = mqttPort;
	}

	@JsonProperty
	public String getOrg() {
		return org;
	}

	@JsonProperty
	public void setOrg(String org) {
		this.org = org;
	}

	@JsonProperty
	public String getApiKey() {
		return apiKey;
	}

	@JsonProperty
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	@JsonProperty
	public String getApiToken() {
		return apiToken;
	}

	@JsonProperty
	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append("host: " + this.mqttHost + ", port: " + this.mqttPort + ", apiKey: " + apiKey + ", token: " + apiToken + ", org: " + org);
		
		return b.toString();
	}
}
