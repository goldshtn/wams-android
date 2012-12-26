package net.sashag.wams.android;

/**
 * Represents an authentication provider that you can use with a Windows Azure Mobile Service.
 * Before you can use a provider, make sure you enabled it in the mobile service's configuration
 * and provided the necessary account details.
 * 
 * @author Sasha Goldshtein
 *
 */
public enum MobileServiceAuthenticationProvider {
	/**
	 * Authenticate with Microsoft Account (formerly Windows Live).
	 */
	MICROSOFT("microsoft"),
	
	/**
	 * Authenticate with Twitter.
	 */
	TWITTER("twitter"),
	
	/**
	 * Authenticate with Facebook.
	 */
	FACEBOOK("facebook"),
	
	/**
	 * Authentication with Google Account.
	 */
	GOOGLE("google");
	
	private final String name;
	
	MobileServiceAuthenticationProvider(String name) {
		this.name = name;
	}
	
	String getName() {
		return name;
	}
}
