package net.sashag.wams.android;

public interface MobileServiceCallback {
	void completedSuccessfully();
	void errorOccurred(MobileException exception);
}
