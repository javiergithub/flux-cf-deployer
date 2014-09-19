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

import static org.eclipse.flux.client.MessageConstants.CF_CONTROLLER_URL;
import static org.eclipse.flux.client.MessageConstants.CF_PUSH_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_SPACE;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_TOKEN;
import static org.eclipse.flux.client.MessageConstants.PROJECT_NAME;
import static org.eclipse.flux.client.MessageConstants.USERNAME;

import java.io.File;
import java.net.URI;
import java.util.Date;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.RequestResponseHandler;
import org.eclipse.flux.client.config.FluxConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

public class CfDeploymentService {
	
	private FluxClient fluxClient = FluxClient.DEFAULT_INSTANCE;
	private FluxConfig fluxConf;
	private MessageConnector flux;
	private AppLogManager appLogs = new AppLogManager();
	
	public CfDeploymentService(FluxClient client, FluxConfig fc) {
		this.fluxClient = client;
		this.fluxConf = fc;
	}

	private CloudFoundryClientDelegate getCfClient(JSONObject req) throws Exception {
		DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(req.getString(CF_TOKEN));
		//We must set expiration or CF client will try to refresh it and throw an NPE because it has no
		// refresh token. See https://github.com/cloudfoundry/cf-java-client/issues/214
		Date nextYear = new Date();
		nextYear.setYear(nextYear.getYear()+1);
		token.setExpiration(nextYear);
		return new CloudFoundryClientDelegate(
			req.getString(USERNAME),
			token,
			new URI(req.getString(CF_CONTROLLER_URL)).toURL(),
			getStringMaybe(req, CF_SPACE),
			flux,
			appLogs
		);
	}
	
	
	private String getStringMaybe(JSONObject req, String key) throws JSONException {
		if (req.has(key)) {
			return req.getString(key);
		}
		return null;
	}

	public void start() {
		this.flux = fluxClient.connect(fluxConf);
		flux.connectToChannel(fluxConf.getUser());

		flux.addMessageHandler(new RequestResponseHandler(flux, CF_SPACES_REQUEST) {
			@Override
			protected JSONObject fillResponse(String type, JSONObject req,
					JSONObject res) throws Exception {
				CloudFoundryClientDelegate cf = getCfClient(req);
				res.put(CF_SPACES, cf.getSpaces());
				return res;
			}

		});
		
		flux.addMessageHandler(new RequestResponseHandler(flux, CF_PUSH_REQUEST) {
			protected JSONObject fillResponse(String type, JSONObject req,
					JSONObject res) throws Exception {
				final String projectName = req.getString(PROJECT_NAME);
				final CloudFoundryClientDelegate cfClient = getCfClient(req);
				final String username = cfClient.getFluxUser();
				FluxClient.executor.execute(new Runnable() {
					public void run() {
						//From here on down work is lenghty and gets done async with any errors sent to the
						// application log (via flux messages).
						DownloadProject downloader = new DownloadProject(flux, projectName, username);
						cfClient.logMessage("Fetching project '"+projectName+"' from Flux...", projectName);
						downloader.run(new DownloadProject.CompletionCallback() {
							@Override
							public void downloadFailed() {
								cfClient.handleError(null, "Fetching project '"+projectName+"' failed", projectName);
								System.err.println("download project failed");
							}
							@Override
							public void downloadComplete(File project) {
								try {
									cfClient.logMessage("Fetching project '"+projectName+"' from Flux - completed", projectName);
									cfClient.push(projectName, project);
								} catch (Throwable e) {
									//Not sending to cfClient logs because we assume that it is itself responsible for
									// doing that for errors produced within any operations it executes.
									e.printStackTrace();
								}
							}
						});
					}
				});
				return res;
			}
		});
	}

	/**
	 * Nobody calls this right now... but anyhow
	 */
	public void shutdown() {
		MessageConnector flux = this.flux;
		if (flux!=null) {
			this.flux = null;
			flux.disconnect();
		}
	}
}
