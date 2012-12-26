package net.sashag.wams.android;

/**
 * Represents a callback for the login operation to the mobile service.
 * 
 * @author Sasha Goldshtein
 *
 */
public interface MobileServiceLoginCallback {

	/**
	 * Called if the login sequence completed successfully. 
	 * 
	 * @param user	the user object representing the currently logged-in user
	 */
	void completedSuccessfully(MobileUser user);
	
	/**
	 * Called if an error occurred during the login sequence.
	 * 
	 * @param exception	the error that occurred
	 */
	void errorOccurred(MobileException exception);
	
	/**
	 * Called if the operation was cancelled by the user.
	 */
	void cancelled();
	
}
