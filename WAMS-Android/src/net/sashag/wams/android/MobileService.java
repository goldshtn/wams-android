package net.sashag.wams.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

/**
 * The entry point to Windows Azure Mobile Services. Represents a single
 * mobile service. Use this class to access your service's data.
 * 
 * @author Sasha Goldshtein
 *
 */
public class MobileService {

	private static final String WAMS_PREFS_NAME = "WAMSPreferences";
	private static final String AUTH_TOKEN_PREF_NAME = "auth_token";
	private static final String USER_ID_PREF_NAME = "user_id";
	
	private String serviceUrl;
	private Context context;
	private MobileUser currentUser;
	private final HttpRequestDecorator requestDecorator = new HttpRequestDecorator();
	
	/**
	 * Initializes a new mobile service entry point with a service URL and API key
	 * retrieved from the specified context's resources. The service URL resource 
	 * should be a string resource titled "mobileServiceUrl", and the API key should
	 * be a string resource titled "mobileServiceApiKey".
	 * 
	 * @param ctx		your application's context (or activity)
	 */
	public MobileService(Context ctx) {
		this(ctx, ResourceUtils.getUrl(ctx), ResourceUtils.getApiKey(ctx));
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
		requestDecorator.setApplicationKey(apiKey);
		readLoginInfo();
		if (currentUser != null) {
			requestDecorator.setAuthenticationToken(currentUser.getAuthenticationToken());
		}
	}
	
	/**
	 * Retrieves a mobile table to access data in your mobile service.
	 * 
	 * @param clazz		the type of elements in the mobile table, a POJO annotated with {@link DataTable DataTable}
	 * @return			an instance of {@link MobileTable MobileTable} for accessing data
	 */
	public <E> MobileTable<E> getTable(Class<E> clazz) {
		return new MobileTable<E>(context, requestDecorator, serviceUrl, clazz);
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
		GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context); //This step is not required in production, but doesn't hurt
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
	 * @param pushCallback	the callback that is invoked on your application's UI thread when a
	 * 						push notification arrives; you can modify your UI directly from it
	 */
	public void registerPushWithTransientCallback(MobileServicePushCallback pushCallback) {
		registerPush();
		TransientPushCallbacks.registerTransientPushCallback(pushCallback);
	}

	/**
	 * Displays an authentication dialog for the specified mobile service authentication provider,
	 * and calls the specified callback when the authentication flow completes successfully. To use
	 * an authentication provider for authentication, you must first configure your mobile service
	 * to support authentication with that provider. Typical configuration involves providing an
	 * API key in the Windows Azure Management Portal.
	 * 
	 * @param provider	the authentication provider
	 * @param callback	the callback invoked when the login flow completes successfully, or 
	 * 					encounters an error
	 */
	public void login(MobileServiceAuthenticationProvider provider, final MobileServiceLoginCallback callback) {
		final AuthenticationWebViewDialog authDialog = new AuthenticationWebViewDialog(context);
		authDialog.start(serviceUrl, provider, new Runnable() {
			public void run() {
				if (authDialog.hasError()) {
					callback.errorOccurred(new MobileException("Error while authenticating: " + authDialog.getError()));
				} else if (authDialog.wasCancelled()) {
					callback.cancelled();
				} else {
					currentUser = authDialog.getUser();
					requestDecorator.setAuthenticationToken(currentUser.getAuthenticationToken());
					persistLoginInfo();
					callback.completedSuccessfully(currentUser);
				}
			}
		});
	}
	
	private void persistLoginInfo() {
		SharedPreferences prefs = context.getSharedPreferences(WAMS_PREFS_NAME, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString(AUTH_TOKEN_PREF_NAME, currentUser.getAuthenticationToken());
		editor.putString(USER_ID_PREF_NAME, currentUser.getUserId());
		editor.commit();	
	}
	
	private void clearLoginInfo() {
		SharedPreferences prefs = context.getSharedPreferences(WAMS_PREFS_NAME, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.remove(AUTH_TOKEN_PREF_NAME);
		editor.remove(USER_ID_PREF_NAME);
		editor.commit();
	}
	
	private void readLoginInfo() {
		SharedPreferences prefs = context.getSharedPreferences(WAMS_PREFS_NAME, Context.MODE_PRIVATE);
		String authToken = prefs.getString(AUTH_TOKEN_PREF_NAME, null);
		String userId = prefs.getString(USER_ID_PREF_NAME, null);
		if (authToken != null && userId != null) {
			currentUser = new MobileUser(authToken, userId);
		}
	}
	
	/**
	 * Logs out the current user. This only clears the authentication cache for your application,
	 * and does not modify any information on the server. If you call the {@link login} method
	 * again, the user may see a very brief authentication window, because the authentication
	 * provider might still remember his credentials.
	 */
	public void logout() {
		if (currentUser != null) {
			requestDecorator.clearAuthenticationToken();
			currentUser = null;
			clearLoginInfo();
		}
	}
	
	/**
	 * Retrieves the currently logged-in user, or <b>null</b> if no user is currently logged in.
	 * 
	 * @return	the currently logged-in user
	 */
	public MobileUser getCurrentUser() {
		return currentUser;
	}
	
	/**
	 * Determines whether a user is currently logged-in.
	 * 
	 * @return	whether a user is currently logged-in
	 */
	public boolean isLoggedIn() {
		return currentUser != null;
	}
	
	//TODO: ctor that takes an authentication token or alt. login method
	
}
