package net.sashag.wams.android;

public class MobileException extends Exception {

	private static final long serialVersionUID = 727615030167047005L;
	
	public MobileException(String message) {
		super(message);
	}
	
	public MobileException(String message, Throwable cause) {
		super(message, cause);
	}

}
