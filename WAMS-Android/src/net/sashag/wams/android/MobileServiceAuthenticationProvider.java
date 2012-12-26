package net.sashag.wams.android;

public enum MobileServiceAuthenticationProvider {
	MICROSOFT("microsoft"),
	TWITTER("twitter"),
	FACEBOOK("facebook"),
	GOOGLE("google");
	
	private final String name;
	
	MobileServiceAuthenticationProvider(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
