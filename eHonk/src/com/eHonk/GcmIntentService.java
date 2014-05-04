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

package com.eHonk;

import com.eHonk.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
	public static final int NOTIFICATION_ID = 13136;

	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that GCM will
			 * be extended in the future with new message types, just ignore any
			 * message types you're not interested in, or that you don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				sendNotification("Send error: " + extras.toString(), "Error title");
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendNotification("Deleted messages on server: " + extras.toString(), "Error title");
				// If it's a regular GCM message, do some work.
			} else if (Constants.LABEL_NOTIFY_MESSAGE.equals(messageType)) {
				String msg;
				final String timestamp = (String) extras
				    .get(Constants.PROPERTY_OFFENSE_TIMESTAMP);
				final String offended_license = (String) extras
				    .get(Constants.PROPERTY_OFFENDED_LICENSE_PLATE);
				if (offended_license.isEmpty()) {
					msg = getApplicationContext().getString(
					    R.string.offense_notification_content1);
				} else {
					msg = getApplicationContext().getString(
					    R.string.offense_notification_content2)
					    + " " + offended_license;
				}
				sendNotification(msg, getApplicationContext().getString(R.string.ehonk_notification_title));
			} else if (Constants.LABEL_UNKNOWNDRIVER_MESSAGE.equals(messageType)) {
				String msg;
				final String offender_license = (String) extras
				    .get(Constants.PROPERTY_OFFENDER_LICENSE_PLATE);
				msg = String.format(getApplicationContext().getString(R.string.offense_notification_content3), offender_license);
				sendNotification(msg, getApplicationContext().getString(R.string.ehonk_notification_title2));
			}
			else {
					Log.i(MainActivity.TAG, "Received message: " + extras.toString());
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	private void sendNotification(String msg, String title) {
		mNotificationManager = (NotificationManager) this
		    .getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		    new Intent(this, MainActivity.class), 0);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		    .setSmallIcon(R.drawable.ic_launcher)
		    .setContentTitle(title)
		    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
		    .setContentText(msg);

		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}
}
