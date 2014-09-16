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

import java.util.Collection;

public class DeploymentConfig {

	private String fluxProjectName;
	private String orgSpace = null;
	
	private String org = null;
	private String space = null;
	
	public DeploymentConfig(String fluxProjectName) {
		this.fluxProjectName = fluxProjectName;
	}
		
	public String getFluxProjectName() {
		return fluxProjectName;
	}

	public String getOrgSpace() {
		return orgSpace;
	}

	public void setCfOrgSpace(String orgSpace) {
		String[] pieces = orgSpace.split("/");
		this.org = pieces[0];
		this.space = pieces[1];
		this.orgSpace = orgSpace;
	}
	
	@Override
	public String toString() {
		return "[Deploy fluxProject '"+fluxProjectName+" to space '"+orgSpace+"']";
	}

	public String getOrg() {
		return org;
	}
	
	public String getSpace() {
		return space;
	}
}
