package org.springframework.social.showcase.cloudfoundry;

import java.util.concurrent.TimeoutException;

import org.eclipse.flux.client.util.ExceptionUtil;

public class CloudFoundryErrors {

	public static String errorMessage(Throwable e) {
		e = ExceptionUtil.getDeepestCause(e);
		if (e instanceof TimeoutException) {
			return "CloudFoundry deployment service is not responding";
		} else {
			return ExceptionUtil.getMessage(e);
		}
	}

}
