package net.sashag.wams.android;

import android.content.Intent;

public interface MobileServicePushCallback {

	void onPushMessageReceived(Intent intent);
	
}
