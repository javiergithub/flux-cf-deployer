/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.social.showcase.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.social.SocialWebAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.showcase.flux.support.Flux;
import org.springframework.social.showcase.flux.support.FluxConnectionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social connectivity with
 * Twitter.
 * 
 * @author Craig Walls
 * @author Kris De Volder
 * @since ???
 */
@Configuration
@AutoConfigureBefore(SocialWebAutoConfiguration.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class FluxDeployerConfiguration {

//	@Configuration
//	@EnableSocial
//	@ConditionalOnWebApplication
//	protected static class GitAutoConfigurationAdapter extends SocialConfigurerAdapter implements
//	EnvironmentAware {
//
//		private RelaxedPropertyResolver properties;
//
//		protected String getPropertyPrefix() {
//			return "spring.social.github.";
//		}
//
//		protected ConnectionFactory<?> createConnectionFactory(RelaxedPropertyResolver properties) {
//			return new GitHubConnectionFactory(properties.getRequiredProperty("appId"),
//					properties.getRequiredProperty("appSecret"));
//		}
//
//		@Override
//		public void addConnectionFactories(ConnectionFactoryConfigurer configurer,
//				Environment environment) {
//			//Copied from spring social code. Dont really understand why it can't just use 'environment'
//			// parameter here instead of implementing 'EnvironmentAware'
//			configurer.addConnectionFactory(createConnectionFactory(this.properties));
//		}
//		
//		@Bean
//		@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
//		public GitHub github(ConnectionRepository repository) {
//			Connection<GitHub> connection = repository.findPrimaryConnection(GitHub.class);
//			if (connection != null) {
//				return connection.getApi();
//			}
//// The twitter one has something like this: do we need that for github? It seems to work fine without this
////			String id = getProperties().getRequiredProperty("app-id");
////			String secret = getProperties().getRequiredProperty("app-secret");
////			return new GitHubTemplate(id, secret);
//			throw new RuntimeException("No github connection.");
//		}
//
//		@Override
//		public void setEnvironment(Environment environment) {
//			this.properties = new RelaxedPropertyResolver(environment, getPropertyPrefix());
//		}
//
//// The example doesn't seem to need this so commented out. 		
////		@Bean(name = { "connect/githubConnect", "connect/githubConnected" })
////		@ConditionalOnProperty(prefix = "spring.social.", value = "auto-connection-views")
////		public View githubConnectView() {
////			return new GenericConnectionStatusView("github", "GitHub");
////		}
//
//	}

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	protected static class FluxConfigrationAdapter extends SocialConfigurerAdapter {

		protected ConnectionFactory<?> createConnectionFactory(Environment environment) {
			return new FluxConnectionFactory(
					environment.getRequiredProperty("cfd.flux.host"),
					environment.getRequiredProperty("cfd.flux.github.client.id"),
					environment.getRequiredProperty("cfd.flux.github.client.secret")
			);
		}

		@Override
		public void addConnectionFactories(ConnectionFactoryConfigurer configurer,
				Environment environment) {
			configurer.addConnectionFactory(createConnectionFactory(environment));
		}
		
		@Bean
		@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
		public Flux flux(ConnectionRepository repository) {
			Connection<Flux> connection = repository.findPrimaryConnection(Flux.class);
			if (connection != null) {
				return connection.getApi();
			}
			throw new RuntimeException("No github connection.");
		}

//		@Override
//		public void setEnvironment(Environment environment) {
//			this.properties = new RelaxedPropertyResolver(environment, getPropertyPrefix());
//		}

// The example doesn't seem to need this so commented out. 		
//		@Bean(name = { "connect/githubConnect", "connect/githubConnected" })
//		@ConditionalOnProperty(prefix = "spring.social.", value = "auto-connection-views")
//		public View githubConnectView() {
//			return new GenericConnectionStatusView("github", "GitHub");
//		}

	}
	
}
