package net.sashag.wams.android;

/**
 * Represents a callback for an asynchronous mobile service operation that does not return a value.
 * 
 * @author Sasha Goldshtein
 *
 */
public interface MobileServiceCallback {
	
	/**
	 * Called when the operation completes successfully.
	 */
	void completedSuccessfully();
	
	/**
	 * Called when the operation throws an exception.
	 * 
	 * @param exception the exception that occurred
	 */
	void errorOccurred(MobileException exception);
}
