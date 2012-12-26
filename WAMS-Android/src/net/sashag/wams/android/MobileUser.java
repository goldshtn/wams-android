package net.sashag.wams.android;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a user logged in using one of the supported authentication providers
 * (see {@link MobileServiceAuthenticationProvider}).
 * 
 * @author Sasha Goldshtein
 *
 */
public class MobileUser {

	private String authenticationToken;
	private String userId;
	
	MobileUser(String userJsonString) throws MobileException {
		try {
			JSONObject userJson = new JSONObject(userJsonString);
			JSONObject userJsonSubObject = userJson.getJSONObject("user");
			userId = userJsonSubObject.getString("userId");
			authenticationToken = userJson.getString("authenticationToken");
		} catch (JSONException ex) {
			throw new MobileException("Error deserializing user object", ex);
		}
	}
	
	MobileUser(String authToken, String userId) {
		this.authenticationToken = authToken;
		this.userId = userId;
	}

	/**
	 * Retrieves the authentication token used to authorize this user to access
	 * the mobile service.
	 * 
	 * @return	the authentication token
	 */
	public String getAuthenticationToken() {
		return authenticationToken;
	}
	
	/**
	 * Retrieves the user id provided by the Windows Azure Mobile Service.
	 * In your server-side scripts, you can use this to identify the user
	 * across multiple log-in sessions on multiple devices.
	 * 
	 * @return	the user id
	 */
	public String getUserId() { 
		return userId;
	}
	
}
