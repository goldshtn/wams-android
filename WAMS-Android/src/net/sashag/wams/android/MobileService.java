package net.sashag.wams.android;

import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.util.Log;

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
	private Context context;
	
	/**
	 * Initializes a new mobile service entry point with a service URL and API key
	 * retrieved from the specified context's resources. The service URL resource 
	 * should be a string resource titled "mobileServiceUrl", and the API key should
	 * be a string resource titled "mobileServiceApiKey".
	 * 
	 * @param ctx		your application's context (or activity)
	 */
	public MobileService(Context ctx) {
		context = ctx;
		serviceUrl = ResourceUtils.getUrl(context);
		apiKey = ResourceUtils.getApiKey(context);
	}
	
	/**
	 * Initializes a new mobile service entry point with a service URL and API key.
	 * Obtain these values from the Windows Azure Management Portal, and store them
	 * securely.
	 * 
	 * @param context		your application's context (or activity)
	 * @param serviceUrl	the URL of your mobile service, e.g. "http://rentahome.azure-mobile.net"
	 * @param apiKey		your service's API key, obtained from the management portal
	 */
	public MobileService(Context context, String serviceUrl, String apiKey) {
		this.context = context;
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
	
	public void unregisterPush() {
		GCMRegistrar.unregister(context);
	}
	
	public void registerPush() {
		//TODO: verify that the "pushChannels" table exists
		
		GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context); //TODO: this step is not required in production
        String registrationId = GCMRegistrar.getRegistrationId(context);
        if (registrationId.equals("")) {
        	String senderId = ResourceUtils.getSenderId(context);
        	GCMRegistrar.register(context, senderId);
        } else {
        	Log.v("MobileService", "Already registered to GCM");
        }
	}
	
	public void registerPushWithTransientCallback(MobileServicePushCallback pushCallback) {
		registerPush();
		TransientPushCallbacks.registerTransientPushCallback(pushCallback);
	}
	
}
