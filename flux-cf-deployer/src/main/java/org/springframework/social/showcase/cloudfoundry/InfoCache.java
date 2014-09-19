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
package org.springframework.social.showcase.cloudfoundry;

/**
 * A trival map-like cache that only keeps one key-value pair.  If a new pair is put in the old one is
 * lost.
 * 
 * @author Kris De Volder
 */
public class InfoCache<K,V> {

	private K key;
	private V value;

	public synchronized void put(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public synchronized V get(K key) {
		if (key.equals(this.key)) {
			return value;
		}
		return null;
	}

}
