package net.sashag.wams.android;

public interface MobileServiceLoginCallback {

	void completedSuccessfully(MobileUser user);
	
	void errorOccurred(MobileException exception);
	
}
