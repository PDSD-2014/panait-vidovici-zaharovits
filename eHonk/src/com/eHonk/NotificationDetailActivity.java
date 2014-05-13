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
			PlaceholderFragment fragment = new PlaceholderFragment();
			fragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
			    .add(R.id.container, fragment).commit();
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

		public PlaceholderFragment() {
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
						String timestamp = data.getString(Constants.PROPERTY_OFFENSE_TIMESTAMP);
						Date offense_date = Constants.iso8601Format.parse(timestamp);
						long endTime = offense_date.getTime() + Constants.TIMEOUT;
						if(System.currentTimeMillis() > endTime)
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
					/* first stop service, to process another message */
					Intent serviceIntent = new Intent(getActivity(),
					    NotificationRecvIntentService.class);
					getActivity().stopService(serviceIntent);
					/* cancel activity */
					getActivity().finish();
				}
			}.execute(null, null, null);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		    Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_notification_detail,
			    container, false);

			Bundle bundle = getArguments();

			String license_plate = bundle
			    .getString(Constants.PROPERTY_OFFENDED_LICENSE_PLATE);
			if (!license_plate.isEmpty()) {
				TextView twNotificationDetail1 = (TextView) rootView
				    .findViewById(R.id.recvNotificationDetail1);
				twNotificationDetail1.setText(getActivity().getApplicationContext()
				    .getString(R.string.recv_notification_title11));
				TextView twNotificationDetail2 = (TextView) rootView
				    .findViewById(R.id.recvNotificationDetail2);
				twNotificationDetail2.setVisibility(View.VISIBLE);
				twNotificationDetail2.setText(license_plate);
			}

			Button btnYes = (Button) rootView.findViewById(R.id.yesButton);
			Button btnNo = (Button) rootView.findViewById(R.id.noButton);

			btnNo.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Bundle bundle = new Bundle();
					bundle.putString(Constants.PROPERTY_MESSAGE_TYPE,
					    Constants.LABEL_NOTIFY_RESPONSE_MESSAGE);
					bundle.putString(Constants.PROPERTY_RESPONSE_TYPE,
					    Constants.VALUE_RESPONSE_IGNORE);
					bundle.putString(
					    Constants.PROPERTY_OFFENDER_LICENSE_PLATE,
					    getArguments().getString(
					        Constants.PROPERTY_OFFENDER_LICENSE_PLATE));
					bundle.putString(Constants.PROPERTY_OFFENDED_GCM_ID, getArguments()
					    .getString(Constants.PROPERTY_OFFENDED_GCM_ID));
					bundle.putString(Constants.PROPERTY_OFFENSE_TIMESTAMP, getArguments()
					    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
					sendResponseToOffended(bundle);
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
					bundle.putString(
					    Constants.PROPERTY_OFFENDER_LICENSE_PLATE,
					    getArguments().getString(
					        Constants.PROPERTY_OFFENDER_LICENSE_PLATE));
					bundle.putString(Constants.PROPERTY_OFFENDED_GCM_ID, getArguments()
					    .getString(Constants.PROPERTY_OFFENDED_GCM_ID));
					bundle.putString(Constants.PROPERTY_OFFENSE_TIMESTAMP, getArguments()
					    .getString(Constants.PROPERTY_OFFENSE_TIMESTAMP));
					sendResponseToOffended(bundle);
				}
			});

			return rootView;
		}
	}

}
