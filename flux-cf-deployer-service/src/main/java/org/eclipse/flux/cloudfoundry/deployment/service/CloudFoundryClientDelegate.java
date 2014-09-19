package org.eclipse.flux.cloudfoundry.deployment.service;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.json.JSONObject;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Defines API for Cloud Foundry operations, like pushing an application.
 */
public class CloudFoundryClientDelegate {

	private static final String DEPLOYMENT_SERVICE_LABEL = "[Deployment Service]";
	private String fluxUser;

	private URL cloudControllerUrl;
	private OAuth2AccessToken token;
	private String orgSpace; // org + "/" + space

	private CloudFoundryClient client;
	private String[] spaces;

	private final MessageConnector connector;
	private AppLogManager appLogs;

	public CloudFoundryClientDelegate(String fluxUser, OAuth2AccessToken token, URL cloudControllerUrl, 
			String space, MessageConnector connector, AppLogManager appLogs) {
		this.fluxUser = fluxUser;
		this.cloudControllerUrl = cloudControllerUrl;
		this.client = createClient(token, cloudControllerUrl, space);
		this.connector = connector;
		this.appLogs = appLogs;
	}

	private CloudFoundryClient createClient(OAuth2AccessToken token,
			URL cloudControllerUrl, String orgSpace) {
		if (orgSpace != null) {
			String[] pieces = getOrgSpace(orgSpace);
			String org = pieces[0];
			String space = pieces[1];
			return new CloudFoundryClient(
					new CloudCredentials(token), cloudControllerUrl,
					org, space);
		} else {
			return new CloudFoundryClient(
					new CloudCredentials(token), cloudControllerUrl);
		}
	}
	
	public String getFluxUser() {
		return fluxUser;
	}

	private String[] getOrgSpace(String orgSpace) {
		return orgSpace.split("/");
	}

	public synchronized void push(String appName, File location) throws Exception {
		CloudFoundryClient client = this.client;
		final CloudFoundryApplication localApp = new CloudFoundryApplication(
				appName, location, client);

		String deploymentName = localApp.getName();

		new ApplicationOperation<Void>(deploymentName,
				"Pushing application") {

			@Override
			protected Void doRun(CloudFoundryClient client)
					throws Exception {

				CloudApplication existingApp = getExistingApplication(getAppName());

				if (existingApp == null) {
					create(localApp);

				} else {
					stopApplication(existingApp);
				}

				upload(localApp);
				start(localApp);

				return null;
			}
		}.run(client);

	}

	protected CloudApplication getExistingApplication(String appName)
			throws Exception {

		return new ApplicationOperation<CloudApplication>(appName,
				"Checking if application exists") {

			protected CloudApplication doRun(CloudFoundryClient client)
					throws Exception {
				List<CloudApplication> apps = client.getApplications();

				if (apps != null) {
					for (CloudApplication deployedApp : apps) {
						if (deployedApp.getName().equals(getAppName())) {
							return deployedApp;
						}
					}
				}
				return null;
			}
		}.run(this.client);

	}

	protected void create(final CloudFoundryApplication app) throws Exception {
		new ApplicationOperation<Void>(app.getName(), "Creating application") {

			protected Void doRun(CloudFoundryClient client) throws Exception {
				client.createApplication(getAppName(),
						new Staging(null, app.getBuildpack()), app.getMemory(),
						app.getUrls(), app.getServices());
				return null;
			}

		}.run(this.client);
	}

	protected void upload(CloudFoundryApplication app) throws Exception {
		final File location = app.getLocation();
		new ApplicationOperation<Void>(app.getName(),
				"Uploading application resources") {

			protected Void doRun(CloudFoundryClient client) throws Exception {
				String appName = getAppName();
				removeLogListener(appName);
				addLogListener(appName);
				client.uploadApplication(appName, location);
				return null;
			}

			protected void onError(Throwable t) {
				removeLogListener(getAppName());
			}

		}.run(this.client);
	}

	protected void start(CloudFoundryApplication app) throws Exception {
		new ApplicationOperation<Void>(app.getName(), "Starting Application") {

			protected Void doRun(CloudFoundryClient client) {
				client.startApplication(getAppName());
				return null;
			}

		}.run(this.client);
	}

	protected void addLogListener(String appName) {
		if (appName != null) {
			String appKey = appKey(appName);
			handleMessage(DEPLOYMENT_SERVICE_LABEL
					+ " - Initialising Loggregator support for - " + appName
					+ '\n', MessageConstants.CF_STREAM_SERVICE_OUT, appName);
			DeployedApplicationLogListener logListener = new DeployedApplicationLogListener(appName);
			appLogs.put(appKey, logListener);
		}
	}

	private String appKey(String appName) {
		StringBuilder key = new StringBuilder();
		String url = this.cloudControllerUrl.toString();
		key.append(url);
		if (!url.endsWith("/")) {
			key.append("/");
		}
		key.append(this.orgSpace);
		key.append("/");
		key.append(appName);
		return key.toString();
	}

	protected void removeLogListener(String appName) {
		DeployedApplicationLogListener logHandler = appLogs.remove(appName);
		if (logHandler != null) {
			handleMessage(DEPLOYMENT_SERVICE_LABEL
					+ " - Removing Loggregator support for - " + appName
					+ '\n', MessageConstants.CF_STREAM_SERVICE_OUT, appName);
			logHandler.dispose();
		}
	}

	protected void stopApplication(final CloudApplication app) throws Exception {
		if (app != null && app.getState() != AppState.STOPPED) {
			new ApplicationOperation<Void>(app.getName(),
					"Stopping application") {

				protected Void doRun(CloudFoundryClient client)
						throws Exception {
					client.stopApplication(app.getName());
					return null;
				}
			}.run(this.client);
		}
	}

	public String getSpace() {
		return orgSpace;
	}

	public synchronized void setSpace(String space) {
		try {
			if (equal(this.orgSpace, space) && client != null) {
				return;
			}
			this.orgSpace = space;
			client = createClient(token, cloudControllerUrl, space);
		} catch (Throwable e) {
			// something went wrong, if we still have a client, its pointing at
			// the wrong space. So...
			// get rid of that client.
			handleError(e, null, null);
			client = null;
		}
	}

	private boolean equal(String s1, String s2) {
		if (s1 == null) {
			return s1 == s2;
		} else {
			return s1.equals(s2);
		}
	}

	public synchronized String[] getSpaces() {
		// We cache this. Assume it really never changes (or at least very
		// rarely).
		if (this.spaces == null) {
			this.spaces = fetchSpaces();
		}
		return this.spaces;
	}

	private String[] fetchSpaces() {
		List<CloudSpace> spaces = client.getSpaces();
		if (spaces != null) {
			String[] array = new String[spaces.size()];
			for (int i = 0; i < array.length; i++) {
				CloudSpace space = spaces.get(i);
				array[i] = space.getOrganization().getName() + "/"
						+ space.getName();
			}
			return array;
		}
		return new String[0];
	}

	protected void handleMessage(String message, String streamType,
			String appName) {
		try {

//			System.out.println(message);

			if (connector != null) {
				JSONObject json = new JSONObject();
				json.put(MessageConstants.USERNAME, this.fluxUser);
				json.put(MessageConstants.CF_APP, appName);

				if (this.orgSpace != null) {
					String[] pieces = getOrgSpace(this.orgSpace);
					String org = pieces[0];
					String space = pieces[1];
					json.put(MessageConstants.CF_ORG, org);
					json.put(MessageConstants.CF_SPACE, space);
				}
				json.put(MessageConstants.CF_MESSAGE, message);
				json.put(MessageConstants.CF_STREAM, streamType);
				System.out.println(streamType+": "+message);

				connector.send(MessageConstants.CF_APP_LOG, json);
			}

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected void handleError(Throwable error, String message, String appName) {
		StringWriter writer = new StringWriter();
		writer.append(DEPLOYMENT_SERVICE_LABEL + " - Error");

		if (appName != null) {
			writer.append(" - ");
			writer.append(appName);
		}

		if (message != null) {
			writer.append(" - ");
			writer.append(message);
		}

		if (error != null) {
			writer.append(" - ");
			writer.append(error.getMessage());
			if (error instanceof CloudFoundryException) {
				String desc = ((CloudFoundryException)error).getDescription();
				if (desc!=null) {
					writer.append(" (");
					writer.append(desc);
					writer.append(")");
				}
			}
		}
		writer.append('\n');

		handleMessage(writer.toString(),
				MessageConstants.CF_STREAM_CLIENT_ERROR, appName);
	}

	class DeployedApplicationLogListener implements ApplicationLogListener {

		private final String appName;
		private StreamingLogToken logToken;

		public DeployedApplicationLogListener(String appName) {
			this.appName = appName;
			this.logToken = CloudFoundryClientDelegate.this.client.streamLogs(appName, this);
		}

		public void dispose() {
			StreamingLogToken token = logToken;
			if (token!=null) {
				token.cancel();
				this.logToken = null;
			}
		}
		
		private boolean isDisposed() {
			return logToken==null;
		}

		public void onComplete() {
			// Nothing for now
		}

		public void onError(Throwable error) {
			CloudFoundryClientDelegate.this.handleError(error, null, appName);
		}

		public void onMessage(ApplicationLog log) {
			if (log != null && !isDisposed()) {
				org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType type = log
						.getMessageType();
				String streamType = null;
				if (type != null) {
					switch (type) {
					case STDERR:
						streamType = MessageConstants.CF_STREAM_STDERROR;
						break;
					case STDOUT:
						streamType = MessageConstants.CF_STREAM_STDOUT;
						break;
					}
				}
				handleMessage(log.getMessage(), streamType, appName);
			}
		}

	}

	/**
	 * 
	 * Performs a {@link CloudFoundryClient} operation. Handles error that may
	 * occur when performing the client call
	 *
	 * @param <T>
	 *            Operation value. java.lang.Void if not returning anything.
	 */
	abstract class ApplicationOperation<T> {

		private final String appName;

		private final String operationName;

		public ApplicationOperation(String appName, String operationName) {
			this.appName = appName;
			this.operationName = operationName;
		}

		public String getAppName() {
			return this.appName;
		}

		/**
		 * 
		 * @param client
		 * @return value of operation. May be null if a successful application
		 *         operation does not need to return a value.
		 * @throws Exception
		 *             if error occurred while performing operation.
		 */
		public T run(CloudFoundryClient client) throws Exception {
			try {
				logMessage(DEPLOYMENT_SERVICE_LABEL + " - " + operationName);
				T val = doRun(client);
				logMessage(DEPLOYMENT_SERVICE_LABEL + " - Completed - "
						+ operationName);
				return val;
			} catch (Throwable t) {
				onError(t);
				logError(t, "Unable to complete operation - " + operationName);
				Exception e = t instanceof Exception ? (Exception) t
						: new Exception(t);
				throw e;
			}
		}

		abstract protected T doRun(CloudFoundryClient client) throws Exception;

		protected void onError(Throwable t) {

		}; 

		protected void logError(Throwable error, String message) {
			handleError(error, message, getAppName());
		}

		protected void logMessage(String message) {
			CloudFoundryClientDelegate.this.handleMessage(message + '\n',
					MessageConstants.CF_STREAM_SERVICE_OUT, getAppName());
		}
		
		
	}

	/**
	 * Sends a message to the log associated with given appName
	 */
	public void logMessage(String message, String appName) {
		handleMessage(DEPLOYMENT_SERVICE_LABEL + " - " + message, MessageConstants.CF_STREAM_SERVICE_OUT, appName);
	}

}
