/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nobledesignlabs.lookupaddress;

import java.util.Random;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.nobledesignlabs.lookupaddress.R;
import com.nobledesignlabs.utils.CommonStuff;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {

	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder builder;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	// public static final String TAG = "GCM Demo";

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */

			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
					.equals(messageType)) {
				// displayNotification(this,"Send error: " + extras);
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
					.equals(messageType)) {
				// displayNotification(this,"Deleted messages on server: "
				// + extras);
				// If it's a regular GCM message, do some work.
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
					.equals(messageType)) {
				// This loop represents the service doing some work.
				// Post notification of received message.
				displayNotification(this, extras);
				Log.i(CommonStuff.TAG, "Received: " + extras.toString());
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	private void displayNotification(Context context, Bundle extras) {
		// ---PendingIntent to launch activity if the user selects
		// this notification---
		// NotificationView context = new NotificationView();
		try {

			Intent i = null;
			Random rand = new Random();
			int timenow = rand.nextInt();
			String title = extras.getString("title");
			String message = extras.getString("message");
			String picture = extras.getString("imgurl");
			String sinfotype = extras.getString("infotype");
			boolean cancelonclick = true;
			int infotype = Integer.parseInt(sinfotype);
			if (infotype == CommonStuff.ADDRESS_NOTIFICATION) {
				i = new Intent(context, NotificationView.class);
				i.putExtra("notificationID", timenow);
				i.putExtra("message", message);
				i.putExtra("title", title);
				i.putExtra("picture", picture);
				cancelonclick = true;
				// i.putExtra("infotype", picture);
			} else if (infotype == CommonStuff.ADDRESS_AUTHORIZATION_REQUEST) {
				i = new Intent(context, AuthorizationRequestActivity.class);
				String token = extras.getString("token");
				i.putExtra("notificationID", timenow);
				i.putExtra("message", message);
				i.putExtra("title", title);
				i.putExtra("picture", picture);
				i.putExtra("token", token);
				String address = extras.getString("address");
				i.putExtra("address", address);
				cancelonclick = true;
				// i.putExtra("infotype", picture);
			} else if (infotype == CommonStuff.ADDRESS_SHARING_REQUEST) {
				i = new Intent(context, AuthorizedActivity.class);
				i.putExtra("notificationID", timenow);
				i.putExtra("message", message);
				i.putExtra("title", title);
				String token = extras.getString("token");
				String address = extras.getString("address");
				i.putExtra("address", address);
				i.putExtra("token", token);
				cancelonclick = false;
				// i.putExtra("infotype", picture);
			}
			if (i != null) {
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, timenow, i, 0);
				/*
				 * NotificationManager nm = (NotificationManager) context
				 * .getSystemService(Context.NOTIFICATION_SERVICE); Notification
				 * notif = new Notification( R.drawable.direction_uturn,
				 * message, timenow); // String title =
				 * context.getString(R.string.app_name);
				 * 
				 * i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
				 * Intent.FLAG_ACTIVITY_SINGLE_TOP);
				 * 
				 * notif.setLatestEventInfo(context, title, message,
				 * pendingIntent); // notif.flags |=
				 * Notification.FLAG_AUTO_CANCEL; notif.flags =
				 * Notification.FLAG_AUTO_CANCEL; notif.defaults |=
				 * Notification.DEFAULT_SOUND; notif.defaults |=
				 * Notification.DEFAULT_VIBRATE; notif.vibrate = new long[] {
				 * 100, 250, 100, 500 }; nm.notify(timenow, notif);
				 */

				NotificationCompat.Builder b = new NotificationCompat.Builder(
						context);

				if (cancelonclick) {
					b.setAutoCancel(true)
							.setOngoing(false)
							.setWhen(System.currentTimeMillis())
							.setSmallIcon(R.drawable.direction_uturn)
							.setTicker(title)
							.setContentTitle(title)
							.setContentText(message)
							.setVibrate(new long[] { 100, 250, 100, 500 })
							.setDefaults(
									Notification.DEFAULT_LIGHTS
											| Notification.DEFAULT_VIBRATE
											| Notification.DEFAULT_SOUND)
							.setContentIntent(pendingIntent)
							.setLights(0xFFF7BF05, 250, 500)
							.setContentInfo("me@address");
					
				} else {
					b.setAutoCancel(false)
					.setOngoing(true)
					.setWhen(System.currentTimeMillis())
					.setSmallIcon(R.drawable.direction_uturn)
					.setTicker(title)
					.setContentTitle(title)
					.setContentText(message)
					.setLights(0xFF308036, 250, 500)
					.setVibrate(new long[] { 100, 250, 100, 500 })
					.setDefaults(
							Notification.DEFAULT_LIGHTS
									| Notification.DEFAULT_VIBRATE
									| Notification.DEFAULT_SOUND)
					.setContentIntent(pendingIntent)
					.setContentInfo("me@address");
				}

				NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				manager.notify(timenow, b.build());
			}
		} catch (Exception d) {
		}
	}

	public void onError(Context context, String errorId) {
		Log.i(CommonStuff.TAG, "Received error: " + errorId);
		// CommonStuff.displayMessage(context, getString(R.string.gcm_error,
		// errorId));
	}
}
