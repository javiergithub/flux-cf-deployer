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

import static org.eclipse.flux.client.MessageConstants.*;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.oauth2.JsonUtil;
import org.cloudfoundry.client.lib.oauth2.OauthClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.util.CompletionAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.social.showcase.flux.support.SingleResponseHandler;
import org.springframework.web.client.RestTemplate;

public class CloudFoundry {

	private URL cloudControllerUrl;
	private String authorizationUrl;
	private String user;
	private MessageConnector flux;
	
	private DeploymentManager deployments = DeploymentManager.INSTANCE;
	private String[] spaces;
	private String space = null;
	
	private RestTemplate rest;
	private OauthClient oauthClient;
	private static InfoCache<URL, Map<String,Object>> infoCache = new InfoCache<>();

	public CloudFoundry(MessageConnector flux, String cloudControllerUrl, RestTemplate rest) throws Exception {
		this.flux = flux;
		this.cloudControllerUrl = new URI(cloudControllerUrl).toURL();
		this.rest = rest;
		this.authorizationUrl = computeAuthorizationUrl(this.cloudControllerUrl);
		
	}

	
	private String computeAuthorizationUrl(URL cloudControllerUrl) {
		Map<String, Object> infoMap = getInfoMap(cloudControllerUrl);
		return (String) infoMap.get("authorization_endpoint");
	}


	private Map<String, Object> getInfoMap(URL cloudControllerUrl) {
		Map<String, Object> info = infoCache.get(cloudControllerUrl);
		if (info!=null) {
			return info;
		}

		String json = rest.getForObject(cloudControllerUrl + "/info", String.class);
		info = JsonUtil.convertJsonToMap(json);
		infoCache.put(cloudControllerUrl, info);
		return info;
	}
	
	/**
	 * Use Rest calls (OAuthClient) to verify user credentials and obtain an OAuth 
	 * access token.
	 */
	public void login(String login, String password, String space) throws Exception {
		try {
			OauthClient client = new OauthClient(new URL(authorizationUrl), rest);
			client.init(new CloudCredentials(login, password));
			oauthClient = client;
			this.user = login;
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
		this.oauthClient = null;
		this.space = null;
		this.user = null;
	}

	public String getUser() {
		return user;
	}

	public String[] getSpaces() {
		if (this.spaces!=null) {
			return spaces;
		}
		try {
			if (!isLoggedIn()) {
				throw new IllegalStateException("Not logged in to CF");
			}
			JSONObject msg = new JSONObject()
				.put(USERNAME, flux.getUser())
				.put(CF_CONTROLLER_URL, this.cloudControllerUrl.toString())
				.put(CF_TOKEN, oauthClient.getToken().getValue());
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
			.put(CF_CONTROLLER_URL, this.cloudControllerUrl.toString())
			.put(USERNAME, flux.getUser())
			.put(CF_TOKEN, oauthClient.getToken().getValue()) //Note this token may expire within 30 minutes.
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
		if (isLoggedIn()) {
			this.space = space;
		} else {
			throw new IllegalStateException("Not logged in");
		}
	}

	public void deploymentChanged(DeploymentConfig config) {
		// TODO Auto-generated method stub
		
	}

	public boolean isLoggedIn() {
		return oauthClient!=null;
	}


}
