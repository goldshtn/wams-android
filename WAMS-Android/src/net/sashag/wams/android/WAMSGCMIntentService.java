package net.sashag.wams.android;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;

/**
 * An intent service that handles GCM registrations, unregistrations, and push messages.
 * You extend this service and override the {@link WAMSGCMIntentService.onPushMessage} method
 * to specify how to handle push messages even if the app is not running when the push
 * notification is received. 
 * 
 * @author Sasha Goldshtein
 *
 */
public class WAMSGCMIntentService extends GCMBaseIntentService {

	private static final String NOTIFICATION_PAYLOAD = "payload";
	private static final String NOTIFICATION_LAUNCH_ACTION = "action";
	private static final String NOTIFICATION_NUMBER = "number";
	private static final String NOTIFICATION_TICKER_TEXT = "tickerText";
	private static final String NOTIFICATION_CONTENT_TEXT = "contentText";
	private static final String NOTIFICATION_CONTENT_TITLE = "contentTitle";
	private static final String BUILT_IN_TYPE_NOTIFICATION = "notification";
	private static final String TOAST_TEXT = "text";
	private static final String BUILT_IN_TYPE_TOAST = "toast";
	private static final String BUILT_IN_TYPE = "__builtInType";

	@Override
	protected final String[] getSenderIds(Context context) {
		String senderId = ResourceUtils.getSenderId(context);
		return new String[] { senderId };
	}

	@Override
	protected final void onRegistered(Context context, String regId) {
		MobileService service = new MobileService(context);
		MobileTable<PushChannel> channelTable = service.getTable(PushChannel.class);
		PushChannel pushChannel = new PushChannel(regId);
		try {
			//NOTE: the server-side script is responsible for making sure there is just one
			//		entry for the specified regId
			channelTable.insert(pushChannel);
			Log.v("GCMIntentService", "Registered regId to server: " + regId);
		} catch (MobileException e) {
			Log.e("GCMIntentService", "Error registering regId: " + regId, e);
		}
	}

	@Override
	protected final void onUnregistered(Context context, String regId) {
		MobileService service = new MobileService(context);
		MobileTable<PushChannel> channelTable = service.getTable(PushChannel.class);
		try {
			List<PushChannel> channels = channelTable.where().equal("regId", regId).select();
			for (PushChannel channel : channels) {
				channelTable.delete(channel);
			}
			Log.v("GCMIntentService", "Unregistered regId from server: " + regId);
		}
		catch (MobileException e) {
			Log.e("GCMIntentService", "Error unregistering regId: " + regId, e);
		}
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.v("GCMIntentService", "Error in register or unregister: " + errorId);
	}

	@Override
	protected final void onMessage(Context context, final Intent intent) {
		Log.v("GCMIntentService", "Received push message: " + intent.toString());
		if (!processBuiltInNotification(context, intent)) {
			runOnMainThread(context, new Runnable() {
				public void run() {
					TransientPushCallbacks.invokePushCallbacks(intent);
					onPushMessage(intent);
				}
			});
		}
	}
	
	private static void runOnMainThread(Context context, Runnable runnable) {
		Handler handler = new Handler(context.getMainLooper());
		handler.post(runnable);
	}
	
	private boolean processBuiltInNotification(final Context context, Intent intent) {
		if (intent.hasExtra(BUILT_IN_TYPE)) {
			String builtInType = intent.getStringExtra(BUILT_IN_TYPE);
			Log.v("GCMIntentService", "Got built-in push notification of type: " + builtInType);
			if (builtInType.equals(BUILT_IN_TYPE_TOAST)) {
				final String text = intent.getStringExtra(TOAST_TEXT);
				runOnMainThread(context, new Runnable() {
					public void run() {
						Toast.makeText(context, text, Toast.LENGTH_LONG).show();
					}
				});
			} else if (builtInType.equals(BUILT_IN_TYPE_NOTIFICATION)) {
				int appIconId = context.getApplicationInfo().icon;
				String contentTitle = intent.getStringExtra(NOTIFICATION_CONTENT_TITLE);
				String contentText = intent.getStringExtra(NOTIFICATION_CONTENT_TEXT);
				String tickerText = intent.getStringExtra(NOTIFICATION_TICKER_TEXT);
				NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
				builder.setSmallIcon(appIconId);
				builder.setContentTitle(contentTitle);
				builder.setContentText(contentText);
				builder.setTicker(tickerText);
				builder.setAutoCancel(true);
				if (intent.hasExtra(NOTIFICATION_NUMBER)) {
					String number = intent.getStringExtra(NOTIFICATION_NUMBER);
					builder.setNumber(Integer.parseInt(number));
				}
				Intent launchIntent;
				if (intent.hasExtra(NOTIFICATION_LAUNCH_ACTION)) {
					String action = intent.getStringExtra(NOTIFICATION_LAUNCH_ACTION);
					launchIntent = new Intent(action);
					if (intent.hasExtra(NOTIFICATION_PAYLOAD)) {
						String payload = intent.getStringExtra(NOTIFICATION_PAYLOAD);
						launchIntent.putExtra(NOTIFICATION_PAYLOAD, payload);
					}
				} else {
					//Default to the application's main activity
					launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
				}
				PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				builder.setContentIntent(pendingIntent);
				
				Notification notification = builder.build();
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.notify(1, notification);
			} else {
				Log.i("GCMIntentService", "Unrecognized built-in notification type: " + builtInType);
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * This method is invoked when a new push message arrives, regardless of whether the
	 * application is running or not. Override this method to specify how to handle new
	 * push messages. You cannot override other methods of this class. This method is invoked
	 * on your application's main thread, so you can manipulate UI from it.
	 * 
	 * @param intent	the intent that represents the push notification; any push parameters
	 * 					passed are available as extras on the intent
	 */
	protected void onPushMessage(Intent intent) {}

}
