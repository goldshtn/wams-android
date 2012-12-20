package net.sashag.wams.android;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public abstract class WAMSGCMIntentService extends GCMBaseIntentService {

	@Override
	protected String[] getSenderIds(Context context) {
		String senderId = ResourceUtils.getSenderId(context);
		return new String[] { senderId };
	}

	@Override
	protected void onRegistered(Context context, String regId) {
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
	protected void onUnregistered(Context context, String regId) {
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
	protected void onMessage(Context context, Intent intent) {
		Log.v("GCMIntentService", "Received push message: " + intent.toString());
		TransientPushCallbacks.invokePushCallbacks(intent);
		onPushMessage(intent);
	}
	
	//This method can be overriden by derived classes to process push notifications even if the
	//app was not running
	protected void onPushMessage(Intent intent) {
	}

}
