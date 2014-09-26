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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.social.github.api.GitHubUserProfile;
import org.springframework.social.github.api.impl.GitHubTemplate;

/**
 * Trying to create a Flux connector similar to Github/Twitter etc connector in spring social.
 */
public class FluxImpl implements Flux, DisposableBean {

	private static final String USERNAME = "username";
	private static final String GET_PROJECTS_REQUEST = "getProjectsRequest";
	private static final String GET_PROJECTS_RESPONSE = "getProjectsResponse";

	private GitHubTemplate github;
	private String accessToken;
	private MessageConnector connector;
	
	private String username; // cached username (avoid rest request for github to fetch it each time
	private String host;
	private FluxClient fluxClient = FluxClient.DEFAULT_INSTANCE;

	public FluxImpl(String accessToken, String host) {
		this.accessToken = accessToken;
		this.github = new GitHubTemplate(accessToken);
		this.host = host;
	}

	@Override
	public GitHubUserProfile getUserProfile() {
		return github.userOperations().getUserProfile();
	}
	
	public String getAccessToken() {
		return accessToken;
	}

	@Override
	public List<String> getProjects() throws Exception {
		MessageConnector conn = getMessagingConnector();
		SingleResponseHandler<List<String>> rh = new SingleResponseHandler<List<String>>(
				getMessagingConnector(), 
				GET_PROJECTS_RESPONSE,
				getUsername()
		) {
			@Override
			protected List<String> parse(JSONObject message) throws Exception {
				//Example message:
				//{"projects":[
				//		{"name":"bikok"},
				//		{"name":"hello-world"}
				//	],
				//	"username":"kdvolder",
				//	"requestSenderID":"amq.gen-55217jjvJI3cJMF9-DZR4A"
				//}
				List<String> projects = new ArrayList<String>();
				JSONArray jps = message.getJSONArray("projects");
				for (int i = 0; i < jps.length(); i++) {
					projects.add(jps.getJSONObject(i).getString("name"));
				}
				return projects;
			}
			
		};
		conn.send(GET_PROJECTS_REQUEST, new JSONObject()
			.put(USERNAME, getUsername()
		));
		return rh.awaitResult();
	}

	@Override
	public synchronized MessageConnector getMessagingConnector() throws Exception {
		if (connector==null) {
			this.connector = fluxClient.connect(
					host,
					getUsername(),
					getAccessToken()
			);
			this.connector.connectToChannelSync(getUsername());
		}
		return connector;
	}

	private String getUsername() {
		if (username==null) {
			username = getUserProfile().getLogin();
		}
		return username;
	}

	@Override
	public void destroy() throws Exception {
		MessageConnector c = connector;
		if (c!=null) {
			connector = null;
			c.disconnect();
		}
		accessToken = null;
		host = null;
		fluxClient= null;
		github = null;
		username = null;
	}

}
