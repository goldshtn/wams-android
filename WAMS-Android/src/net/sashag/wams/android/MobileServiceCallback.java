package net.sashag.wams.android;

/**
 * Represents a callback for an asynchronous mobile service operation that does not return a value.
 * 
 * @author Sasha Goldshtein
 *
 */
public interface MobileServiceCallback<E> {
	
	/**
	 * Called when the operation completes successfully.
	 * 
	 * @param item the item affected by the operation
	 */
	void completedSuccessfully(E item);
	
	/**
	 * Called when the operation throws an exception.
	 * 
	 * @param exception the exception that occurred
	 */
	void errorOccurred(MobileException exception);
}
