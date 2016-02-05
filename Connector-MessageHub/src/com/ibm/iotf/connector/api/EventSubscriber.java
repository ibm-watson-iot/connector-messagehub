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

package com.ibm.iotf.connector.api;

public abstract class EventSubscriber {
	
	// the object that will handle the republishing of
	// events that are received by this object
	private EventRepublisher publisher;
	
	/**
	 * Configures the object that will handle the
	 * republishing of events received by this object
	 * @param publisher
	 */
	public void setPublisher(EventRepublisher publisher) {
		this.publisher = publisher;
	}
	
	/**
	 * hands off the event to the configured publisher (if
	 * there is one).  This method should be called by concrete
	 * implementations of this class to republish events.
	 * 
	 * @param jsonEventStr
	 */
	public void handleEvent(String eventKey, String jsonEventStr) {
		if (this.publisher != null) {
			this.publisher.publishEvent(eventKey, jsonEventStr);
		}
	}
}
