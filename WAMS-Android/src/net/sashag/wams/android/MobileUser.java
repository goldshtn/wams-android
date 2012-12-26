package net.sashag.wams.android;

import org.json.JSONException;
import org.json.JSONObject;

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
	
	public String getAuthenticationToken() {
		return authenticationToken;
	}
	
	public String getUserId() { 
		return userId;
	}
	
}
