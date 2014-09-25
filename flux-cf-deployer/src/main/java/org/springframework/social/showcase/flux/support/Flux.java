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
package org.springframework.social.showcase.flux.support;

import java.util.List;

import org.eclipse.flux.client.MessageConnector;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.social.github.api.GitHubUserProfile;

public interface Flux extends DisposableBean {

	GitHubUserProfile getUserProfile();

	String getAccessToken();

	List<String> getProjects() throws Exception;

	MessageConnector getMessagingConnector();
	
}
