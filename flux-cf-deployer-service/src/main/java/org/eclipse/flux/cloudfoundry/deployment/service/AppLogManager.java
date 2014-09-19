/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.cloudfoundry.deployment.service;

import java.util.HashMap;

import org.eclipse.flux.cloudfoundry.deployment.service.CloudFoundryClientDelegate.DeployedApplicationLogListener;

/**
 * Keeps track of app log listeners per deployed application. This to ensure we have only one log listener
 * per-app sending messages to flux bus.
 * 
 * @author Kris De Volder
 */
public class AppLogManager {

	private HashMap<String, DeployedApplicationLogListener> activeListeners;
	
	/**
	 * Insert a new log listener. If a log listener already exists for this key
	 * it is automatically removed and disposed.
	 */
	public synchronized void put(String appKey, DeployedApplicationLogListener logListener) {
		if (activeListeners==null) {
			activeListeners = new HashMap<String, CloudFoundryClientDelegate.DeployedApplicationLogListener>();
		} else {
			//only do this if activeListeners map is not new. If map is new, this is pointless since map is empty.
			DeployedApplicationLogListener existing = activeListeners.get(appKey);
			if (existing!=null) {
				existing.dispose();
			}
		}
		activeListeners.put(appKey, logListener);
	}
	
	/**
	 * Get log listener if it exists. Returns null otherwise.
	 */
	public synchronized DeployedApplicationLogListener get(String appKey) {
		if (activeListeners!=null) {
			return activeListeners.get(appKey);
		}
		return null;
	}

	/**
	 * Remove log listener if it exists. Returns the removed log listener or
	 * null.
	 */
	public synchronized DeployedApplicationLogListener remove(String appKey) {
		if (activeListeners!=null) {
			return activeListeners.remove(appKey);
		}
		return null;
	}

}
