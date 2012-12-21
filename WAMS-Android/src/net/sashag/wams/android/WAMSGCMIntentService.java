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
		if (intent.hasExtra("builtInType")) {
			String builtInType = intent.getStringExtra("builtInType");
			if (builtInType.equals("toast")) {
				final String text = intent.getStringExtra("text");
				runOnMainThread(context, new Runnable() {
					public void run() {
						Toast.makeText(context, text, Toast.LENGTH_LONG).show();
					}
				});
			} else if (builtInType.equals("notification")) {
				int appIconId = context.getApplicationInfo().icon;
				String contentTitle = intent.getStringExtra("contentTitle");
				String contentText = intent.getStringExtra("contextText");
				String tickerText = intent.getStringExtra("tickerText");
				NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
				builder.setSmallIcon(appIconId);
				builder.setContentTitle(contentTitle);
				builder.setContentText(contentText);
				builder.setTicker(tickerText);
				if (intent.hasExtra("number")) {
					String number = intent.getStringExtra("number");
					builder.setNumber(Integer.parseInt(number));
				}
				Intent launchIntent;
				if (intent.hasExtra("action")) {
					String action = intent.getStringExtra("action");
					launchIntent = new Intent(action);
					if (intent.hasExtra("payload")) {
						String payload = intent.getStringExtra("payload");
						launchIntent.putExtra("payload", payload);
					}
				} else {
					//Default to the application's main activity
					launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
				}
				//TODO: Take care of TaskStackBuilder stuff ...
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
