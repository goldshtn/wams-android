package net.sashag.wams.android;

import android.content.Context;
import android.content.res.Resources;

public class ResourceUtils {
	
	private static final String PUSH_SENDER_RESOURCE_ID = "mobileServicePushSenderId";
	private static final String API_KEY_RESOURCE_ID = "mobileServiceApiKey";
	private static final String URL_RESOURCE_ID = "mobileServiceUrl";
	
	public static String getSenderId(Context context) {
		return getStringResourceByName(context, PUSH_SENDER_RESOURCE_ID);
	}
	
	public static String getApiKey(Context context) {
		return getStringResourceByName(context, API_KEY_RESOURCE_ID);
	}
	
	public static String getUrl(Context context) {
		return getStringResourceByName(context, URL_RESOURCE_ID);
	}
	
	private static String getStringResourceByName(Context context, String resourceName) {
		String packageName = context.getPackageName();
		Resources resources = context.getResources();
		int resId = resources.getIdentifier(resourceName, "string", packageName);
		if (resId == 0) {
			throw new Resources.NotFoundException(resourceName);
		}
		return resources.getString(resId);
	}
}
