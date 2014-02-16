package com.nobledesignlabs.utils;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.nobledesignlabs.lookupaddress.NotificationView;
import com.nobledesignlabs.lookupaddress.R;
import com.nobledesignlabs.oauth.Oauth2Params;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public final class CommonStuff {

	public static final String TAG = "me@address";
	public static Oauth2Params OAUTH2PARAMS = Oauth2Params.GOOGLE_PLUS;
	public static final String scope = "https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/plus.me";
	public static String	 serverName="http://23.23.40.51:3000/";
	public static String versionName = "";
	public static int versionNumber = 0;
	public static final String SENDER_ID = ""; //<--- project ID from google Console
	static final String DISPLAY_MESSAGE_ACTION = "com.nobledesignlabs.lookupaddress.DISPLAY_MESSAGE";
	static final String EXTRA_MESSAGE = "message";
	
	public static  final int THUMBNAIL_SIZE=555;
	
	public static  final float DEFAULT_ZOOM = 17f;
	//public static  final int MIN_ZOOM_LVL_TO_ZOOM_OUT = 50;

	public static  final int PUBLIC_ADDRESS = 0;
	public static  final int PROTECTED_ADDRESS = 1;
	public static  final int PRIVATE_ADDRESS = 2;
	
	//public static final int NOTIFICATION_ID = 1;
	public static final int ADDRESS_NOTIFICATION = 1;
	public static final int ADDRESS_AUTHORIZATION_REQUEST = 2;
	public static final int ADDRESS_SHARING_REQUEST = 3;
	
	public static final long MIN_WAIT_TIME = 60000;
	public static final float MIN_DISTANCE = 50;

	public static void displayMessage(Context context, String message) {
		Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
		intent.putExtra(EXTRA_MESSAGE, message);
		context.sendBroadcast(intent);
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	public static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}
	
	public static int convertDpToPixel(float dp, Context context) {
		return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
	}
	
	public static LatLngBounds createLatLngBoundsObject(LatLng firstLocation,
			LatLng secondLocation) {
		if (firstLocation != null && secondLocation != null) {
			LatLngBounds.Builder builder = new LatLngBounds.Builder();
			builder.include(firstLocation).include(secondLocation);

			return builder.build();
		}
		return null;
	}
	
	/**
	 * Shows the keyboard.
	 * 
	 * @param view
	 */
	public static void showKeyboard(View view) {
	    Context context = view.getContext();
	    InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
	}

	/**
	 * Hides the keyboard.
	 * 
	 * @param view
	 */
	public static void hideKeyboard(View view) {
	    Context context = view.getContext();
	    InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
	/*
	public static void turnGPSOn(Context ctx) {

		String provider = android.provider.Settings.Secure.getString(
				ctx.getContentResolver(),
				android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if (!provider.contains("gps")) { // if gps is disabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			ctx.sendBroadcast(poke);
			// ToastLoadShout("Turning GPS on..");
			Toast.makeText(ctx, "Turning GPS on..",
					Toast.LENGTH_SHORT).show();
		}
	}*/
}
