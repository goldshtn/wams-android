package net.sashag.wams.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.util.Log;

import com.google.android.gcm.GCMBroadcastReceiver;

public class WAMSGCMBroadcastReceiver extends GCMBroadcastReceiver {
	@Override
	protected String getGCMIntentServiceClassName(Context context) {
		String packageName = context.getPackageName();
		PackageInfo info;
		try {
			info = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SERVICES);
			for (ServiceInfo service : info.services) {
				Class<?> serviceClass = Class.forName(service.name);
				Class<?> superClass = serviceClass.getSuperclass();
				if (superClass.equals(WAMSGCMIntentService.class)) {
					return serviceClass.getName();
				}
			}
		} catch (NameNotFoundException e) {
			Log.i("WAMSGCMBroadcastReceiver", "Error locating GCM intent service derived class", e);
		} catch (ClassNotFoundException e) {
			Log.i("WAMSGCMBroadcastReceiver", "Error accessing GCM intent service derived class", e);
		}
		return WAMSGCMIntentService.class.getName();
	}
}