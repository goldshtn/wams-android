package net.sashag.wams.android;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;

class TransientPushCallbacks {

	private static List<MobileServicePushCallback> pushCallbacks = new ArrayList<MobileServicePushCallback>();
	
	public static void registerTransientPushCallback(MobileServicePushCallback pushCallback) {
		synchronized(pushCallbacks) {
			pushCallbacks.add(pushCallback);
		}
	}
	
	//TODO: remove API as well, perhaps based on token
	
	public static void invokePushCallbacks(Intent intent) {
		MobileServicePushCallback[] copy;
		synchronized(pushCallbacks) {
			copy = new MobileServicePushCallback[pushCallbacks.size()];
			pushCallbacks.toArray(copy);
		}
		
		for (MobileServicePushCallback callback : copy) {
			callback.onPushMessageReceived(intent);
		}
	}
	
}
