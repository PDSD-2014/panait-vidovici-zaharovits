package com.eHonk;

import java.text.ParseException;
import java.util.Date;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class NotificationRecvIntentService extends IntentService {

	public static final int NOTIFICATION_RECV_ID = 613136;

	public NotificationRecvIntentService() {
		super("NotificationRecvIntentService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		/* process data from intent */
		String msg;
		final String timestamp = intent
		    .getStringExtra(Constants.PROPERTY_OFFENSE_TIMESTAMP);
		final String offended_license = intent
		    .getStringExtra(Constants.PROPERTY_OFFENDED_LICENSE_PLATE);
		if (offended_license.isEmpty()) {
			msg = getApplicationContext().getString(
			    R.string.offense_notification_content1);
		} else {
			msg = getApplicationContext().getString(
			    R.string.offense_notification_content2)
			    + " " + offended_license;
		}

		Date offense_date = null;
		try {
			offense_date = Constants.iso8601Format.parse(timestamp);
		} catch (ParseException e2) {
			e2.printStackTrace();
			return;
		}

		Intent notifyIntent = new Intent(this, NotificationDetailActivity.class);
		notifyIntent.putExtra(Constants.PROPERTY_OFFENSE_TIMESTAMP, timestamp);
		notifyIntent.putExtra(Constants.PROPERTY_OFFENDED_GCM_ID,
		    intent.getStringExtra(Constants.PROPERTY_OFFENDED_GCM_ID));
		notifyIntent.putExtra(Constants.PROPERTY_OFFENDED_LICENSE_PLATE,
		    intent.getStringExtra(Constants.PROPERTY_OFFENDED_LICENSE_PLATE));
		notifyIntent.putExtra(Constants.PROPERTY_OFFENDER_LICENSE_PLATE,
		    intent.getStringExtra(Constants.PROPERTY_OFFENDER_LICENSE_PLATE));

		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		    | Intent.FLAG_ACTIVITY_NO_HISTORY
		    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

		PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0,
		    notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		    .setSmallIcon(R.drawable.ic_launcher)
		    .setContentTitle(
		        getApplicationContext()
		            .getString(R.string.ehonk_notification_title))
		    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
		    .setContentText(msg);

		mBuilder.setContentIntent(notifyPendingIntent);
		startForeground(NOTIFICATION_RECV_ID, mBuilder.build());

		long endTime = offense_date.getTime() + Constants.TIMEOUT;

		/* this stops receiving other messages */
		while (System.currentTimeMillis() < endTime) {
			synchronized (this) {
				try {
					wait(endTime - System.currentTimeMillis());
				} catch (Exception e) {
				}
			}
		}

	}
}
