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

import static org.eclipse.flux.client.MessageConstants.CF_APP;
import static org.eclipse.flux.client.MessageConstants.CF_APP_LOG;
import static org.eclipse.flux.client.MessageConstants.CF_CONTROLLER_URL;
import static org.eclipse.flux.client.MessageConstants.CF_LOGIN_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_LOGIN_RESPONSE;
import static org.eclipse.flux.client.MessageConstants.CF_MESSAGE;
import static org.eclipse.flux.client.MessageConstants.CF_ORG;
import static org.eclipse.flux.client.MessageConstants.CF_PASSWORD;
import static org.eclipse.flux.client.MessageConstants.CF_SPACE;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES_RESPONSE;
import static org.eclipse.flux.client.MessageConstants.CF_STREAM;
import static org.eclipse.flux.client.MessageConstants.CF_STREAM_CLIENT_ERROR;
import static org.eclipse.flux.client.MessageConstants.CF_USERNAME;
import static org.eclipse.flux.client.MessageConstants.OK;
import static org.eclipse.flux.client.MessageConstants.PROJECT_NAME;
import static org.eclipse.flux.client.MessageConstants.USERNAME;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.util.CompletionAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.social.showcase.flux.support.SingleResponseHandler;

public class CloudFoundry {

	private URL cloudControllerUrl;
	private MessageConnector flux;
	private String user;
	private boolean loggedIn = false;
	
	private DeploymentManager deployments = DeploymentManager.INSTANCE;
	private String[] spaces;
	private String space = null;
	private String password;

	public CloudFoundry(MessageConnector flux, String cloudControllerUrl) throws Exception {
		this.flux = flux;
		this.cloudControllerUrl = new URI(cloudControllerUrl).toURL();
	}

	public void login(String login, String password, String space) throws Exception {
		try {
			JSONObject msg = new JSONObject()
				.put(USERNAME, flux.getUser())
				.put(CF_CONTROLLER_URL, cloudControllerUrl.toString())
				.put(CF_USERNAME, login)
				.put(CF_PASSWORD, password)
				.put(CF_SPACE, space);
			SingleResponseHandler<Void> response = new SingleResponseHandler<Void>(flux, CF_LOGIN_RESPONSE, flux.getUser()) {
				@Override
				protected Void parse(JSONObject message) throws Exception {
					boolean ok = message.getBoolean(OK);
					if (!ok) {
						throw new IOException("Login failed for unkownn reason");
					}
					return null;
				}
			};
			flux.send(CF_LOGIN_REQUEST, msg);
			
			response.awaitResult();
			loggedIn = true;
			this.user = login;
			this.password = password;
			this.space = space;
		} catch (Throwable e) {
			logout();
			if (e instanceof Exception) {
				throw (Exception)e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	private void logout() {
		this.loggedIn = false;
		this.user = null;
		this.password = null;
		this.space = null;
	}

	public String getUser() {
		return user;
	}

	public String[] getSpaces() {
		if (this.spaces!=null) {
			return spaces;
		}
		try {
			if (!loggedIn) {
				throw new IllegalStateException("Not logged in to CF");
			}
			JSONObject msg = new JSONObject()
				.put(USERNAME, flux.getUser());
			SingleResponseHandler<String[]> response = new SingleResponseHandler<String[]>(flux, CF_SPACES_RESPONSE, flux.getUser()) {
				@Override
				protected String[] parse(JSONObject message) throws Exception {
					JSONArray _spaces = message.getJSONArray(CF_SPACES);
					String[] spaces = new String[_spaces.length()];
					for (int i = 0; i < spaces.length; i++) {
						spaces[i] = _spaces.getString(i);
					}
					return spaces;
				}
			};
			flux.send(CF_SPACES_REQUEST, msg);
			return this.spaces=response.awaitResult();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public synchronized CfFluxDeployment getFluxDeployment(String fluxProjectName) {
		return deployments.get(user, fluxProjectName);
	}

	public synchronized DeploymentConfig createDefaultDeploymentConfig(String fluxProjectName) {
		return new DeploymentConfig(fluxProjectName);
	}

	public synchronized DeploymentConfig getDeploymentConfig(String fluxProjectName) {
		CfFluxDeployment deployment = deployments.get(flux.getUser(), fluxProjectName);
		if (deployment==null) {
			return createDefaultDeploymentConfig(fluxProjectName);
		} else {
			return deployment.getConfig();
		}
	}

	public synchronized void apply(DeploymentConfig config) {
		String fluxProjectName = config.getFluxProjectName();
		String fluxUser = flux.getUser();
		CfFluxDeployment deployment = deployments.get(fluxUser, fluxProjectName);
		if (deployment==null) {
			deployment = new CfFluxDeployment(this, fluxProjectName);
			deployments.put(fluxUser, deployment);
		}
		deployment.configure(config);
	}

	public void push(final DeploymentConfig config) throws Exception {
		SingleResponseHandler<Void> response = new SingleResponseHandler<Void>(flux, MessageConstants.CF_PUSH_RESPONSE, flux.getUser()) {
			@Override
			protected Void parse(JSONObject message) throws Exception {
				return null;
			}
		};
		flux.send(MessageConstants.CF_PUSH_REQUEST,  new JSONObject()
			.put(USERNAME, flux.getUser())
			.put(CF_SPACE, config.getOrgSpace())
			.put(PROJECT_NAME, config.getFluxProjectName())
		);
		response.getFuture().whenDone(new CompletionAdapter<Void>() {
			@Override
			public void rejected(Throwable e) {
				try {
					flux.send(CF_APP_LOG, new JSONObject()
						.put(USERNAME, flux.getUser())
						.put(CF_APP, config.getFluxProjectName())
						.put(CF_ORG, config.getOrg())
						.put(CF_SPACE, config.getSpace())
						.put(CF_MESSAGE, CloudFoundryErrors.errorMessage(e))
						.put(CF_STREAM, CF_STREAM_CLIENT_ERROR)
					);
				} catch (JSONException e1) {
					e.printStackTrace();
				}
			}
		});

	}

	public String getSpace() {
		return space;
	}

	public void setSpace(String space) throws Exception {
		if (loggedIn) {
			login(this.user, this.password, this.space);
		} else {
			throw new IllegalStateException("Not logged in");
		}
	}

	public void deploymentChanged(DeploymentConfig config) {
		// TODO Auto-generated method stub
		
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}


}
