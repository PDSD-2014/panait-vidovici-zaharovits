package com.eHonk;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class NotificationDetailActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_detail);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
			    .add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.notification_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		final AtomicInteger msgId = new AtomicInteger();
		Database.OffenseRecord offense = getNextOffense(getActivity());
		
		private static Database.OffenseRecord getNextOffense(Context context) {
			
			Database db = Database.getInstance(context);
			Database.OffenseRecord offense = null;

			db.lock.lock();

			try {
				offense = db.getLastOffenses(
				    Database.NOTIFICATIONS_LOG_TABLE_NAME_RECV,
				    Constants.iso8601Format.format(new Date(System.currentTimeMillis()
				        - Constants.TIMEOUT)));

				if (offense == null)
					return null;

				db.removeOffense(Database.NOTIFICATIONS_LOG_TABLE_NAME_RECV, offense);

			} finally {
				db.lock.unlock();
			}
			
			return offense;
		}
		
		private void showOffense(View rootView, Database.OffenseRecord offense) {

			if(offense==null) {
				/* show Toast and close activity */
				Toast.makeText(getActivity(), getActivity().getString(R.string.ehonk_notifications_expired_toast),
				    Toast.LENGTH_LONG).show();
				getActivity().finish();
				return;
			}
			
			final String offended_license_plate = offense.getLicense();
			
			TextView twNotificationDetail1 = (TextView) rootView
			    .findViewById(R.id.recvNotificationDetail1);
			TextView twNotificationDetail2 = (TextView) rootView
			    .findViewById(R.id.recvNotificationDetail2);
			
			if (offended_license_plate.isEmpty()) {
				twNotificationDetail1.setText(getActivity().getApplicationContext()
				    .getString(R.string.recv_notification_title21));
				twNotificationDetail2.setVisibility(View.GONE);
			} else {
				twNotificationDetail1.setText(getActivity().getApplicationContext()
				    .getString(R.string.recv_notification_title11));
				twNotificationDetail2.setVisibility(View.VISIBLE);
				twNotificationDetail2.setText(offended_license_plate);
			}
		}

		private void sendResponseToOffended(Bundle bundle) {

			final Bundle data = bundle;

			new AsyncTask<Void, Void, Boolean>() {
				@Override
				protected Boolean doInBackground(Void... params) {

					Activity activity = PlaceholderFragment.this.getActivity();
					Context context = activity.getApplicationContext();
					GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity);

					/* check time here */
					try {
						String timestamp = data
						    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP);
						Date offense_date = Constants.iso8601Format.parse(timestamp);
						long endTime = offense_date.getTime() + Constants.TIMEOUT;
						if (System.currentTimeMillis() > endTime)
							return false;
					} catch (ParseException e2) {
						e2.printStackTrace();
					}

					long backoff = Constants.BACKOFF_MILLI_SECONDS
					    + Constants.random.nextInt(1000);

					for (int i = 1; i <= Constants.MAX_ATTEMPTS; i++) {
						Log.d(Constants.TAG, "Attempt #" + i + " to register");

						try {
							/* send registration id to backend */
							gcm.send(
							    context.getString(R.string.sender_id) + "@gcm.googleapis.com",
							    Constants.LABEL_NOTIFY_RESPONSE_MESSAGE
							        + msgId.incrementAndGet(), data);

							return true;
						} catch (IOException e) {
							Log.e(Constants.TAG, "Failed to register on attempt " + i + ":"
							    + e);
							if (i == Constants.MAX_ATTEMPTS) {
								break;
							}
							try {
								Log.d(Constants.TAG, "Sleeping for " + backoff
								    + " ms before retry");
								Thread.sleep(backoff);
							} catch (InterruptedException e1) {
								// Activity finished before we complete - exit.
								Log.d(Constants.TAG,
								    "Thread interrupted: abort remaining retries!");
								Thread.currentThread().interrupt();
								return false;
							}
							// increase backoff exponentially
							backoff *= 2;
						}
					}

					return false;
				}

				@Override
				protected void onPostExecute(Boolean msg) {
					if (msg) {
						Toast.makeText(getActivity().getApplicationContext(),
						    "Message sent", Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getActivity().getApplicationContext(),
						    "Message failed to send!", Toast.LENGTH_LONG).show();
					}
				}
			}.execute(null, null, null);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		    Bundle savedInstanceState) {

			final View rootView = inflater.inflate(R.layout.fragment_notification_detail,
			    container, false);

			showOffense(rootView, this.offense);

			Button btnYes = (Button) rootView.findViewById(R.id.yesButton);
			Button btnNo = (Button) rootView.findViewById(R.id.noButton);

			/* get my license_plate (OFFENDER) */
			final String license_plate = MainActivity
			    .getRegistrationLicense(getActivity());

			btnNo.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Bundle bundle = new Bundle();
					bundle.putString(Constants.PROPERTY_MESSAGE_TYPE,
					    Constants.LABEL_NOTIFY_RESPONSE_MESSAGE);
					bundle.putString(Constants.PROPERTY_RESPONSE_TYPE,
					    Constants.VALUE_RESPONSE_IGNORE);
					bundle.putString(Constants.PROPERTY_OFFENDER_LICENSE_PLATE,
					    license_plate);
					bundle.putString(Constants.PROPERTY_OFFENDED_GCM_ID,
					    offense.getGcmId());
					bundle.putString(Constants.PROPERTY_OFFENSE_TIMESTAMP,
					    offense.getTimestamp());
					
					sendResponseToOffended(bundle);
					
					PlaceholderFragment.this.offense = getNextOffense(getActivity());
					if(PlaceholderFragment.this.offense==null)
						getActivity().finish();
					showOffense(rootView, PlaceholderFragment.this.offense);
				}
			});

			btnYes.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Bundle bundle = new Bundle();
					bundle.putString(Constants.PROPERTY_MESSAGE_TYPE,
					    Constants.LABEL_NOTIFY_RESPONSE_MESSAGE);
					bundle.putString(Constants.PROPERTY_RESPONSE_TYPE,
					    Constants.VALUE_RESPONSE_COMING);
					bundle.putString(Constants.PROPERTY_OFFENDER_LICENSE_PLATE,
					    license_plate);
					bundle.putString(Constants.PROPERTY_OFFENDED_GCM_ID,
					    offense.getGcmId());
					bundle.putString(Constants.PROPERTY_OFFENSE_TIMESTAMP,
					    offense.getTimestamp());
					
					sendResponseToOffended(bundle);
					
					PlaceholderFragment.this.offense = getNextOffense(getActivity());
					if(PlaceholderFragment.this.offense==null)
						getActivity().finish();
					showOffense(rootView, PlaceholderFragment.this.offense);
				}
			});


			return rootView;
		}
	}

}
