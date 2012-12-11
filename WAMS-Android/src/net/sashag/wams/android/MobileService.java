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
	
	/**
	 * Initializes a new mobile service entry point with a service URL and API key.
	 * Obtain these values from the Windows Azure Management Portal, and store them
	 * securely.
	 * 
	 * @param serviceUrl	the URL of your mobile service, e.g. "http://rentahome.azure-mobile.net"
	 * @param apiKey		your service's API key, obtained from the management portal
	 */
	public MobileService(String serviceUrl, String apiKey) {
		this.serviceUrl = serviceUrl;
		this.apiKey = apiKey;
	}
	
	/**
	 * Retrieves a mobile table to access data in your mobile service.
	 * 
	 * @param clazz		the type of elements in the mobile table, a POJO annotated with {@link DataTable DataTable}
	 * @return			an instance of {@link MobileTable MobileTable} for accessing data
	 */
	public <E> MobileTable<E> getTable(Class<E> clazz) {
		return new MobileTable<E>(serviceUrl, apiKey, clazz);
	}
	
}
