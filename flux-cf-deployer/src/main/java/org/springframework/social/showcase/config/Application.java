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
package org.springframework.social.showcase.config;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.showcase.cloudfoundry.CloudFoundryManager;
import org.springframework.social.showcase.cloudfoundry.CloudFoundryManagerImpl;
import org.springframework.social.showcase.signin.SimpleSignInAdapter;
import org.springframework.web.client.RestTemplate;

@ComponentScan(basePackages="org.springframework.social.showcase")
@EnableConfigurationProperties
@EnableAutoConfiguration
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public SignInAdapter signInAdapter() {
		return new SimpleSignInAdapter(new HttpSessionRequestCache());
	}

	@Bean
	public CloudFoundryManager cfm() {
		return new CloudFoundryManagerImpl();
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public Filter httpsRedirect() {
		//Filter that redirects any http request to https on cloudfoundry.
		return new Filter() {
			
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
			}

			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				if ("http".equals(request.getScheme())) {
					String serverName = request.getServerName();
					if (serverName.endsWith("cfapps.io")) {
						HttpServletRequest req = (HttpServletRequest) request;
						HttpServletResponse res = (HttpServletResponse) response;
						if (response instanceof HttpServletResponse) {
							String redirectTo = req.getRequestURL().toString();
							if (redirectTo.startsWith("http:")) {
								redirectTo = "https:"+redirectTo.substring(5);
								String q = req.getQueryString();
								if (q!=null) {
									redirectTo = redirectTo +"?" + q;
								}
							}
							res.sendRedirect(redirectTo);
							return;
						}
					}
				}
				//Our filter does not apply... pas on to next guy in chain
				chain.doFilter(request, response);
			}

			@Override
			public void destroy() {
			}
			
		};
	}
	
}