package net.sashag.wams.android;

/**
 * Represents an exception that occurred in a mobile service operation.
 * 
 * @author Sasha Goldshtein
 *
 */
public class MobileException extends Exception {

	private static final long serialVersionUID = 727615030167047005L;
	
	/**
	 * Initializes a new instance of this class with an error message.
	 * 
	 * @param message the error message
	 */
	public MobileException(String message) {
		super(message);
	}
	
	/**
	 * Initializes a new instance of this class with an error message and an additional {@link Throwable} cause.
	 * 
	 * @param message	the error message
	 * @param cause		the original cause, an instance of {@link Throwable}
	 */
	public MobileException(String message, Throwable cause) {
		super(message, cause);
	}

}
