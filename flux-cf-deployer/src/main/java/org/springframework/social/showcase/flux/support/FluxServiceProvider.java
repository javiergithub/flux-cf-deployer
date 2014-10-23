/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.showcase.flux.support;

import org.eclipse.flux.client.util.Assert;
import org.springframework.social.oauth2.AbstractOAuth2ServiceProvider;
import org.springframework.social.oauth2.OAuth2Template;

/**
 * Flux ServiceProvider implementation.
 * @author Keith Donald
 * @author Kris De Volder
 */
public class FluxServiceProvider extends AbstractOAuth2ServiceProvider<Flux> {

	private String rabbitURI;
	private String socketIOHost;

	public FluxServiceProvider(String rabbitURI, String socketIOHost, String clientId, String clientSecret) {
		super(createOAuth2Template(clientId, clientSecret));
		Assert.assertTrue(rabbitURI!=null);
		this.rabbitURI = rabbitURI;
		this.socketIOHost = socketIOHost;
	}

	private static OAuth2Template createOAuth2Template(String clientId, String clientSecret) {
		OAuth2Template oAuth2Template = new OAuth2Template(clientId, clientSecret, "https://github.com/login/oauth/authorize", "https://github.com/login/oauth/access_token");
		oAuth2Template.setUseParametersForClientAuthentication(true);
		return oAuth2Template;
	}

	public Flux getApi(String accessToken) {
		return new FluxImpl(accessToken, rabbitURI, socketIOHost);
	} 

}
