package net.sashag.wams.android;

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
