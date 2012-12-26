package net.sashag.wams.android;

import java.net.HttpURLConnection;

class HttpRequestDecorator {

	private static final String APPLICATION_KEY_HEADER_NAME = "X-ZUMO-APPLICATION";
	private static final String AUTH_HEADER_NAME = "X-ZUMO-AUTH";
	private String authenticationToken;
	private String applicationKey;
	
	public String getAuthenticationToken() {
		return authenticationToken;
	}
	
	public void setAuthenticationToken(String token) {
		authenticationToken = token;
	}
	
	public void clearAuthenticationToken() {
		authenticationToken = null;
	}
	
	public void setApplicationKey(String apiKey) {
		applicationKey = apiKey;
	}
	
	public void decorateHttpRequest(HttpURLConnection urlConnection) {
		urlConnection.addRequestProperty(APPLICATION_KEY_HEADER_NAME, applicationKey);
		if (authenticationToken != null) {
			urlConnection.addRequestProperty(AUTH_HEADER_NAME, authenticationToken);
		}
	}

	public void decorateHttpRequest(HttpPatch httpPatch) {
		httpPatch.addHeader(APPLICATION_KEY_HEADER_NAME, applicationKey);
		if (authenticationToken != null) {
			httpPatch.addHeader(AUTH_HEADER_NAME, authenticationToken);
		}
	}
	
}
