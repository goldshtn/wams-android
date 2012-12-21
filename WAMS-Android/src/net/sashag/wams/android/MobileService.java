package net.sashag.wams.android;

import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
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
	
	/**
	 * Unregisters this device from push notifications.
	 */
	public void unregisterPush() {
		GCMRegistrar.unregister(context);
		TransientPushCallbacks.removeAll();
	}
	
	/**
	 * Registers this device for push notifications. To receive push notifications, you must
	 * follow these steps:
	 * <p>
	 * <ol>
	 * <li>Create a mobile table called "pushChannels".</li>
	 * <li>Add your mobile service URL as a string resource titled "mobileServiceUrl".</li>
	 * <li>Add your mobile service API key as a string resource titled "mobileServiceApiKey".</li>
	 * <li>Add your GCM sender ID as a string resource titled "mobileServicePushSenderId".</li>
	 * <li>To handle non-transient push notifications (i.e. to handle push notifications even if the app
	 * is no longer running), extend the {@link WAMSGCMIntentService} class and override the
	 * {@link WAMSGCMIntentService.onPushMessage} method.</li>
	 * </ol>
	 * <p>
	 * To handle push notifications without a class that extends {@link WAMSGCMIntentService}, use
	 * the {@link registerPushWithTransientCallback} method. A callback you register using that method
	 * will not be invoked if your application is no longer running.
	 */
	public void registerPush() {
		//TODO: verify that the "pushChannels" table exists -- either check for exception accessing it,
		//		or use the management API, which can be accessed the same as the azure xPlat CLI tool
		
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
	
	/**
	 * Registers this device for push notifications, and subscribes the specified callback
	 * to push notifications as long as this application is still running. The callback will
	 * not be invoked if the application terminates or if the device restarts. See the 
	 * {@link registerPush} method for what you need to do to handle push notifications in general,
	 * and to handle them in a non-transient way in particular.
	 * 
	 * @param pushCallback	the callback that is invoked on an unspecified thread when a
	 * 						push notification arrives; you cannot modify you UI directly
	 * 						from within this callback (use a {@link Handler} or {@link Activity.runOnUiThread})
	 */
	public void registerPushWithTransientCallback(MobileServicePushCallback pushCallback) {
		registerPush();
		TransientPushCallbacks.registerTransientPushCallback(pushCallback);
	}
	
}
