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

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.eHonk.R;
import com.eHonk.Constants;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
	public static final int NOTIFICATION_RECV_ID = 613136;

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
				sendSimpleNotification("Send error: " + extras.toString(),
				    "Error title", null);
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendSimpleNotification(
				    "Deleted messages on server: " + extras.toString(), "Error title", null);
				/* if it's a regular GCM message, do some work. */
			} else if (Constants.LABEL_NOTIFY_MESSAGE.equals(messageType)) {

				/* check if notification is not due */
				final String timestamp = extras
				    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP);
				Date offense_date = null;
				try {
					offense_date = Constants.iso8601Format.parse(timestamp);
					long endTime = offense_date.getTime() + Constants.TIMEOUT;
					if (endTime < System.currentTimeMillis())
						return; // this notification is already due
				} catch (ParseException e2) {
					e2.printStackTrace();
					return;
				}

				/* insert notification in DB */
				Database db = Database.getInstance(this);
				String tableName = Database.NOTIFICATIONS_LOG_TABLE_NAME_RECV;

				/* next db transactions need to be atomic */
				db.lock.lock();

				try {

					Database.OffenseRecord offense = db.selectOffense(tableName,
					    extras.getString(Constants.PROPERTY_OFFENDED_GCM_ID));

					if (offense == null) {
						/* new offended driver */
						offense = db.new OffenseRecord();
						offense.setLicense(extras
						    .getString(Constants.PROPERTY_OFFENDED_LICENSE_PLATE));
						offense.setGcmId(extras
						    .getString(Constants.PROPERTY_OFFENDED_GCM_ID));
						offense.setTimestamp(extras
						    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
						Integer offenders_count = Integer.parseInt(extras
						    .getString(Constants.PROPERTY_OFFENDERS_COUNT));
						if (offenders_count > 1)
							offense.setTypeCode(Database.NOTIFICATION_TYPE_MULTIRECV);
						else
							offense.setTypeCode(Database.NOTIFICATION_TYPE_RECV);
						offense.setStatusCode(Database.NOTIFICATION_STATUS_NOTIFIED);
						/* extras is the number of offended drivers */
						offense.setOtherDetails(extras
						    .getString(Constants.PROPERTY_OFFENDERS_COUNT));
						offense.setRetriesCount(1);

						db.addOffense(tableName, offense);

					} else {
						/* increment offense count */
						try {
							/* update retries count */
							long prev_off_time = Constants.iso8601Format.parse(
							    offense.getTimestamp()).getTime();
							if ((offense_date.getTime() - prev_off_time) < Constants.TIMEOUT) {
								offense.setRetriesCount(offense.getRetriesCount() + 1);
							} else {
								offense.setRetriesCount(1);
							}
							/* update timestamp */
							offense.setTimestamp(extras
							    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
							offense.setLicense(extras
							    .getString(Constants.PROPERTY_OFFENDED_LICENSE_PLATE));
							Integer offenders_count = Integer.parseInt(extras
							    .getString(Constants.PROPERTY_OFFENDERS_COUNT));
							if (offenders_count > 1)
								offense.setTypeCode(Database.NOTIFICATION_TYPE_MULTIRECV);
							else
								offense.setTypeCode(Database.NOTIFICATION_TYPE_RECV);
							offense.setStatusCode(Database.NOTIFICATION_STATUS_NOTIFIED);
							/* extras is the number of offended drivers */
							offense.setOtherDetails(extras
							    .getString(Constants.PROPERTY_OFFENDERS_COUNT));

							db.updateOffense(tableName, offense);

						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} finally {
					db.lock.unlock();
				}

				String msg;
				final String offended_license = extras
				    .getString(Constants.PROPERTY_OFFENDED_LICENSE_PLATE);
				if (offended_license.isEmpty()) {
					msg = getApplicationContext().getString(
					    R.string.offense_notification_content1);
				} else {
					msg = getApplicationContext().getString(
					    R.string.offense_notification_content2)
					    + " " + offended_license;
				}

				sendFlashyNotification(msg,
				    getApplicationContext()
				        .getString(R.string.ehonk_notification_title));

			} else if (Constants.LABEL_UNKNOWNDRIVER_MESSAGE.equals(messageType)) {
				
				String msg;
				final String offender_license = (String) extras
				    .get(Constants.PROPERTY_OFFENDER_LICENSE_PLATE);
				msg = String.format(
				    getApplicationContext().getString(
				        R.string.offense_notification_content3), offender_license);
				
				Database db = Database.getInstance(this);
				String tableName = Database.NOTIFICATIONS_LOG_TABLE_NAME_SENT;

				/* next db transactions need to be atomic */
				db.lock.lock();
				
				try {
					List<Database.OffenseRecord> offenses = db.selectOffenses(tableName, offender_license);

					if (offenses.isEmpty()) {
						/* new offended driver */
						Database.OffenseRecord offense = db.new OffenseRecord();
						offense.setLicense( offender_license);
						offense.setTimestamp( extras
						    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
						offense.setTypeCode(Database.NOTIFICATION_STATUS_NREG);
						offense.setStatusCode(Database.NOTIFICATION_STATUS_NREG);

						offense.setRetriesCount(1);

						db.addOffense(tableName, offense);
					}
					else {
						for (Database.OffenseRecord offenseRecord : offenses) {
	            offenseRecord.setTimestamp(extras
							    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
	            offenseRecord.setTypeCode(Database.NOTIFICATION_STATUS_NREG);
	            offenseRecord.setStatusCode(Database.NOTIFICATION_STATUS_NREG);
							offenseRecord.setRetriesCount( offenseRecord.getRetriesCount() + 1);
							
							db.updateOffense(tableName, offenseRecord);
            }
					}
				} finally {
					db.lock.unlock();
				}				

				sendSimpleNotification(
				    msg,
				    getApplicationContext().getString(
				        R.string.ehonk_notification_title2), null);

			} else if (Constants.LABEL_NOTIFYACK_MESSAGE.equals(messageType)) {
				
				String msg;
				final String offender_license = (String) extras
				    .get(Constants.PROPERTY_OFFENDER_LICENSE_PLATE);
				final String count_alerted_drivers = (String) extras
				    .get(Constants.PROPERTY_OFFENDERS_COUNT);
				msg = String.format(
				    getApplicationContext().getString(
				        R.string.offense_notification_content4), count_alerted_drivers);
				
				Database db = Database.getInstance(this);
				String tableName = Database.NOTIFICATIONS_LOG_TABLE_NAME_SENT;

				/* next db transactions need to be atomic */
				db.lock.lock();
				
				try {
					List<Database.OffenseRecord> offenses = db.selectOffenses(tableName, offender_license);

					if (offenses.isEmpty()) {
						/* new offended driver */
						Database.OffenseRecord offense = db.new OffenseRecord();
						offense.setLicense( offender_license);
						offense.setTimestamp( extras
						    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
						Integer offenders_count = Integer.parseInt(extras
						    .getString(Constants.PROPERTY_OFFENDERS_COUNT));
						if (offenders_count > 1)
							offense.setTypeCode(Database.NOTIFICATION_TYPE_MULTISENT);
						else
							offense.setTypeCode(Database.NOTIFICATION_TYPE_SENT);
						offense.setStatusCode(Database.NOTIFICATION_STATUS_NOTIFIED);
						/* extras is the number of offended drivers */
						offense.setOtherDetails(extras
						    .getString(Constants.PROPERTY_OFFENDERS_COUNT));
						offense.setRetriesCount(1);

						db.addOffense(tableName, offense);
					}
					else {
						for (Database.OffenseRecord offenseRecord : offenses) {
	            offenseRecord.setTimestamp(extras
							    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
							Integer offenders_count = Integer.parseInt(extras
							    .getString(Constants.PROPERTY_OFFENDERS_COUNT));
							if (offenders_count > 1)
								offenseRecord.setTypeCode(Database.NOTIFICATION_TYPE_MULTISENT);
							else
								offenseRecord.setTypeCode(Database.NOTIFICATION_TYPE_SENT);
							offenseRecord.setStatusCode(Database.NOTIFICATION_STATUS_NOTIFIED);
							/* extras is the number of offended drivers */
							offenseRecord.setOtherDetails(extras
							    .getString(Constants.PROPERTY_OFFENDERS_COUNT));
							offenseRecord.setRetriesCount( offenseRecord.getRetriesCount() + 1);
							
							db.updateOffense(tableName, offenseRecord);
            }
					}
				} finally {
					db.lock.unlock();
				}

				sendSimpleNotification(
				    msg,
				    getApplicationContext().getString(
				        R.string.ehonk_notification_title3), null);
				
			} else if (Constants.LABEL_NOTIFY_RESPONSE_MESSAGE.equals(messageType)) {
				
				String msg = "", title = "";
				final String response = extras
				    .getString(Constants.PROPERTY_RESPONSE_TYPE);
				final String offender_license = extras
				    .getString(Constants.PROPERTY_OFFENDER_LICENSE_PLATE);
				if (response.equals(Constants.VALUE_RESPONSE_COMING)) {
					msg = String.format(
					    getApplicationContext().getString(
					        R.string.yes_response_notification), offender_license);
					title = getApplicationContext().getString(
					    R.string.ehonk_notification_title4);
				} else if (response.equals(Constants.VALUE_RESPONSE_IGNORE)) {
					msg = String.format(
					    getApplicationContext().getString(
					        R.string.no_response_notification), offender_license);
					title = getApplicationContext().getString(
					    R.string.ehonk_notification_title5);
				}
				
				Database db = Database.getInstance(this);
				String tableName = Database.NOTIFICATIONS_LOG_TABLE_NAME_SENT;

				/* next db transactions need to be atomic */
				db.lock.lock();
				
				try {
					List<Database.OffenseRecord> offenses = db.selectOffenses(tableName, offender_license);

					String response_timestamp = Constants.iso8601Format.format(new Date());
					
					if (offenses.isEmpty()) {
						/* new offended driver */
						Database.OffenseRecord offense = db.new OffenseRecord();
						offense.setLicense( offender_license);
						offense.setTimestamp( extras
						    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
						if( response.equals(Constants.VALUE_RESPONSE_COMING)) {
							offense.setStatusCode(Database.NOTIFICATION_STATUS_ACK_OK);
						}
						else if (response.equals(Constants.VALUE_RESPONSE_IGNORE)) {
							offense.setStatusCode(Database.NOTIFICATION_STATUS_ACK_IGN);
						}
						
						/* extras is the response timestamp */
						offense.setOtherDetails( response_timestamp);

						db.addOffense(tableName, offense);
					}
					else {
						for (Database.OffenseRecord offenseRecord : offenses) {
	            offenseRecord.setTimestamp(extras
							    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
							if( response.equals(Constants.VALUE_RESPONSE_COMING)) {
								offenseRecord.setStatusCode(Database.NOTIFICATION_STATUS_ACK_OK);
							}
							else if (response.equals(Constants.VALUE_RESPONSE_IGNORE)) {
								offenseRecord.setStatusCode(Database.NOTIFICATION_STATUS_ACK_IGN);
							}

							/* extras is the number of offended drivers */
							db.updateOffense(tableName, offenseRecord);
            }
					}
				} finally {
					db.lock.unlock();
				}

				Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				sendSimpleNotification(msg, title, alarmSound);

			} else {
				Log.i(Constants.TAG, "Received message: " + extras.toString());
			}
		}
		/* release the wake lock provided by the WakefulBroadcastReceiver. */
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	/* no rings no vibrate notification */
	private void sendSimpleNotification(String msg, String title, Uri alarmSound) {

		NotificationManager mNotificationManager = (NotificationManager) this
		    .getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notifyIntent = new Intent(this, MainActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(notifyIntent);
		notifyIntent.setFlags(0);

		PendingIntent notifyPendingIntent = stackBuilder.getPendingIntent(0,
		    PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		    .setSmallIcon(R.drawable.ic_launcher).setContentTitle(title)
		    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
		    .setAutoCancel(true)
		    .setSound(alarmSound)
		    .setContentText(msg);

		mBuilder.setContentIntent(notifyPendingIntent);

		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	/* this should be more noisy to alert the user */
	private void sendFlashyNotification(String msg, String title) {

		NotificationManager mNotificationManager = (NotificationManager) this
		    .getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notifyIntent = new Intent(this, NotificationDetailActivity.class);

		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		    | Intent.FLAG_ACTIVITY_NO_HISTORY
		    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

		PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0,
		    notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		long[] pattern = {500,500,500,500,500,500,500,500,500,500,500,500,500,500,500};

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		    .setSmallIcon(R.drawable.ic_launcher)
		    .setContentTitle(
		        getApplicationContext()
		            .getString(R.string.ehonk_notification_title))
		    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
		    .setAutoCancel(false)
		    .setOngoing(true)
		    .setLights(Color.BLUE, 500, 500)
		    .setSound(alarmSound)
		    .setVibrate(pattern)
		    .setContentText(msg);

		mBuilder.setContentIntent(notifyPendingIntent);

		mNotificationManager.notify(NOTIFICATION_RECV_ID, mBuilder.build());
	}
}
