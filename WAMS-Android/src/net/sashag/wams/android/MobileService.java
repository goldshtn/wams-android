package net.sashag.wams.android;

/**
 * The entry point to Windows Azure Mobile Services. Represents a single
 * mobile service. Use this class to access your service's data.
 * 
 * @author Sasha Goldshtein
 *
 */
public class MobileService {

	private String serviceUrl;
	private String apiKey;
	
	public MobileService(String serviceUrl, String apiKey) {
		this.serviceUrl = serviceUrl;
		this.apiKey = apiKey;
	}
	
	public <E> MobileTable<E> getTable(Class<E> clazz) {
		return new MobileTable<E>(serviceUrl, apiKey, clazz);
	}
	
}
