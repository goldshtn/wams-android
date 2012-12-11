package net.sashag.wams.android;

import java.util.List;

/**
 * Represents a callback for an asynchronous mobile service operation that returns a list of elements.
 * 
 * @author Sasha Goldshtein
 *
 * @param <E> the type of elements returned by the operation
 */
public interface MobileServiceCallbackWithResults<E> {
	
	/**
	 * Called when the operation completes successfully, with the list of results.
	 * 
	 * @param results the results of the mobile operation
	 */
	void completedSuccessfully(List<E> results);
	
	/**
	 * Called when the operation throws an exception.
	 * 
	 * @param exception the exception that occurred
	 */
	void errorOccurred(MobileException exception);
}
