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

import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.core.env.Environment;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.showcase.flux.support.Flux;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CloudfoundryController {

	private CloudFoundryManager cfm;
	
	@Inject
	public CloudfoundryController(CloudFoundryManager cfm) {
		this.cfm = cfm;
	}
	
	@Inject
	private ConnectionRepository connectionRepository;
	
	@Inject
	private Environment env;

	@RequestMapping("/cloudfoundry/deploy")
	public String deploy(Principal currentUser, Model model) throws Exception {
		try {
			CloudFoundry cf = cfm.getConnection(currentUser);
			if (!isLoggedIn(cf)) {
				return "redirect:/cloudfoundry/login";
			}
			Flux flux = flux();
			if (flux==null) {
				return "redirect:/singin/flux";
			}
			
			String defaultSpace = cf.getSpace();
			
			//The page should show a list of flux project with their deployment status.
			List<String> projects = flux.getProjects();
			model.addAttribute("user", cf.getUser());
			model.addAttribute("projects", projects);
			model.addAttribute("spaces", cf.getSpaces());
			model.addAttribute("defaultSpace", defaultSpace);
			
			if (projects.isEmpty()) {
				model.addAttribute("error_message", "Nothing to deploy: You don't have any Flux projects!");
			}
			
			List<DeploymentConfig> deployments = new ArrayList<DeploymentConfig>();
			for (String pname : projects) {
				DeploymentConfig deployConf = cf.getDeploymentConfig(pname);
				deployments.add(deployConf);
			}
			model.addAttribute("deployments", deployments);
			return "cloudfoundry/deploy";
		} catch (Throwable e) {
			//trouble happens is cf service gets restarted and is no longer in logged in state, but
			// deployer app still thinks that it is. This can be solved by logging in again.
			return "redirect:/cloudfoundry/login?error="+URLEncoder.encode(CloudFoundryErrors.errorMessage(e), "UTF8");
		}
	}

	private boolean isLoggedIn(CloudFoundry cf) {
		return cf!=null && cf.isLoggedIn();
	}

	@RequestMapping(value="/cloudfoundry/deploy.do", method=RequestMethod.POST)
	public String deployDo(Principal currentUser, 
			@RequestParam("project") String project,
			@RequestParam("space") String space,
			Model model) throws Exception {
		try {
			CloudFoundry cf = cfm.getConnection(currentUser);
			if (!isLoggedIn(cf)) {
				return "redirect:/cloudfoundry/login";
			}
			Flux flux = flux();
			if (flux==null) {
				return "redirect:/singin/flux";
			}
			DeploymentConfig dep = new DeploymentConfig(project);
			dep.setCfOrgSpace(space);
			cf.setSpace(space); //use this as default space from now on
			cf.push(dep);
			return "redirect:/cloudfoundry/app-log"
				+"?space="+URLEncoder.encode(space, "UTF-8")
				+"&project="+URLEncoder.encode(project, "UTF-8");
		} catch (Throwable e) {
			e.printStackTrace();
			//Broken cfdeployer service state, or service died. If the service is still there
			// it likely has not been properly logged in for this user so redirect user to login page
			// and show error message there.
			return "redirect:/cloudfoundry/login?error="+URLEncoder.encode(CloudFoundryErrors.errorMessage(e), "UTF8");
		}
	}
	
	private Flux flux() {
		Connection<Flux> connection = connectionRepository.findPrimaryConnection(Flux.class);
		if (connection!=null) {
			return connection.getApi();
		}
		return null;
	}

	@RequestMapping(value="/cloudfoundry")
	public String profile(Principal currentUser, Model model) {
		CloudFoundry cf = cfm.getConnection(currentUser);
		if (!isLoggedIn(cf)) {
			return "redirect:/cloudfoundry/login";
		}
		model.addAttribute("space", cf.getSpace());
		model.addAttribute("user", cf.getUser());
		model.addAttribute("spaces", cf.getSpaces());
		return "cloudfoundry";
	}
	
	@RequestMapping(value="/cloudfoundry/processLogin")
	public String doLogin(Principal currentUser, Model model,
		@RequestParam(required=false, value="cf_login") String login, 
		@RequestParam(required=false, value="cf_password") String password
	) throws Exception {
		Connection<Flux> flux = connectionRepository.findPrimaryConnection(Flux.class);

		CloudFoundry cf = new CloudFoundry(
				flux.getApi().getMessagingConnector(),
				env.getProperty("cloudfoundry.url", "https://api.run.pivotal.io/"));
		try {
			cf.login(login, password, null);
			cfm.putConnection(currentUser, cf);
			return "redirect:/cloudfoundry/deploy";
		} catch (Throwable e) {
			return "redirect:/cloudfoundry/login?error="+URLEncoder.encode(CloudFoundryErrors.errorMessage(e), "UTF8");
		}
	}
	
	@RequestMapping("/cloudfoundry/login")
	public String login() {
		return "cloudfoundry/login";
	}
	
	@RequestMapping("/cloudfoundry/app-log")
	public String appLogs(Principal currentUser, Model model,
		@RequestParam("space") String orgSpace,
		@RequestParam("project") String project
	) {
		Flux flux = flux();
		if (flux==null) {
			return "redirect:/singin/flux";
		}
		String [] pieces = orgSpace.split("/");
		model.addAttribute("org",pieces[0]);
		model.addAttribute("space", pieces[1]);
		model.addAttribute("app", project);
		
		model.addAttribute("fluxUser", flux.getUserProfile().getLogin());
		model.addAttribute("fluxHost", flux.getMessagingConnector().getHost());
		return "cloudfoundry/app-log";
	}

}
