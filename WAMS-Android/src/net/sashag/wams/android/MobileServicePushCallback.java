package net.sashag.wams.android;

import android.content.Intent;

/**
 * Represents a callback for a push notification, called when a push message arrives.
 * Registering for push notifications using this interface is transient; only if the
 * application is still running, the callback will be invoked. To register in a non-transient
 * way, use the {@link MobileService.registerPush} method, extend the {@link WAMSGCMIntentService}
 * class, and override the {@link WAMSGCMIntentService.onPushMessage} method. 
 * 
 * @author Sasha Goldshtein
 *
 */
public interface MobileServicePushCallback {

	/**
	 * The callback that is invoked when a push message arrives. The thread that invokes
	 * this callback is unspecified, and will not be the UI thread of your application.
	 * 
	 * @param intent	the intent that represents the push message; any push parameters passed
	 * 					will be available as extras of this intent
	 */
	void onPushMessageReceived(Intent intent);
	
}
