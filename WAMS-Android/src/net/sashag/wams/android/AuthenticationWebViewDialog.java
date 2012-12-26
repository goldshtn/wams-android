package net.sashag.wams.android;

import java.net.URLDecoder;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class AuthenticationWebViewDialog {
	
	private static final String TOKEN_URL_PART = "#token=";
	private static final String ERROR_URL_PART = "#error=";
	
	private WebView webView;
	private AlertDialog dialog;
	private String error;
	private MobileUser user;
	private boolean cancelled;

	@SuppressLint("SetJavaScriptEnabled")
	AuthenticationWebViewDialog(Context context) {
		webView = new WebView(context);
		WebSettings settings = webView.getSettings();
		settings.setSavePassword(false);
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Please wait...");
		builder.setView(webView);
		
		dialog = builder.create();
	}
	
	boolean wasCancelled() {
		return cancelled;
	}
	
	boolean hasError() {
		return error != null;
	}
	
	String getError() {
		return error;
	}
	
	MobileUser getUser() {
		return user;
	}

	 void start(String serviceUrl, MobileServiceAuthenticationProvider provider, final Runnable done) {
		
		final String startUrl = serviceUrl + "/login/" + provider.getName();
		final String endUrl = serviceUrl + "/login/done";
		
		dialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface d) {
				cancelled = true;
			}
		});
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				dialog.setTitle("Login");
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.v("AuthenticationWebViewDialog", "Navigating to URL: " + url);
				if (url.startsWith(endUrl)) {
					finishLogin(url, done);
					return true;
				}
				return false;
			}
		});
		webView.loadUrl(startUrl);
		dialog.show();
	}
	
	private void finishLogin(String url, Runnable done) {
		int indexOfToken = url.indexOf(TOKEN_URL_PART);
		int indexOfError = url.indexOf(ERROR_URL_PART);
		if (indexOfToken != -1) {
			String respJSON = url.substring(indexOfToken + TOKEN_URL_PART.length());
			respJSON = URLDecoder.decode(respJSON);
			try {
				user = new MobileUser(respJSON);
				error = null;
			} catch (MobileException ex) {
				error = ex.getMessage();
			}
		} else if (indexOfError != -1) {
			user = null;
			String encodedError = url.substring(indexOfError + ERROR_URL_PART.length());
			error = URLDecoder.decode(encodedError);
		}
		dialog.dismiss();
		done.run();
	}
	
}
