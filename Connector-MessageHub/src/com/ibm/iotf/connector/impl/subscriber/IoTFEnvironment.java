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
public class IoTFEnvironment {
	private String name, label, plan;
	private IoTFCredentials credentials;
	private String appId;
	
	@JsonProperty
	public String getName() {
		return name;
	}
	
	@JsonProperty
	public void setName(String name) {
		this.name = name;
	}
	
	@JsonProperty
	public String getLabel() {
		return label;
	}
	
	@JsonProperty
	public void setLabel(String label) {
		this.label = label;
	}
	
	@JsonProperty
	public String getPlan() {
		return plan;
	}
	
	@JsonProperty
	public void setPlan(String plan) {
		this.plan = plan;
	}
	
	@JsonProperty
	public IoTFCredentials getCredentials() {
		return credentials;
	}
	
	@JsonProperty
	public void setCredentials(IoTFCredentials credentials) {
		this.credentials = credentials;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}	
}
