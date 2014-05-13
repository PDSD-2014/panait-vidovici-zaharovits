package com.eHonk;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.eHonk.R;
import com.eHonk.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

@SuppressLint("DefaultLocale")
public class MainActivity extends ActionBarActivity {

	private static final int ENABLE_NETWORK_REQUEST = 1;

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String PROPERTY_IS_REGISTERED = "is_registered";
	private static final String PROPERTY_IS_DRIVER = "is_driver";
	private static final String PROPERTY_APP_VERSION = "app_version"; /*
																																		 * change
																																		 * this for
																																		 * new
																																		 * releases
																																		 * so that
																																		 * apps
																																		 * re-register
																																		 * to GCM
																																		 */

	private AsyncTask<Void, Void, Boolean> gcmRegisterTask = null;

	final AtomicInteger msgId = new AtomicInteger();

	GoogleCloudMessaging gcm;
	Context context;
	String regid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		context = getApplicationContext();

		if (!isOnline()) {
			createNetErrorDialog();
		}

		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(this);
			regid = getRegistrationId(context);

			if (regid.isEmpty() && isOnline()) {
				/* get RegisterId from cloud */
				gcmRegisterTask = registerInBackground();
			}

			if (savedInstanceState == null) {
				android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
				android.support.v4.app.FragmentTransaction ft1 = fm.beginTransaction();
				ft1.add(R.id.container, new NotifyFragment(), "fragment_tag1");
				// ft1.addToBackStack(null);
				ft1.commit();
				if (!isRegistered()) {
					android.support.v4.app.FragmentTransaction ft2 = fm
					    .beginTransaction();
					ft2.replace(R.id.container, new RegisterFragment(), "fragment_tag2");
					ft2.addToBackStack("tag2");
					ft2.commit();
				}
			}
		} else {
			Log.i(Constants.TAG, "No valid Google Play Services APK found.");
		}
	}

	protected void onCreateSecondStage(Bundle savedInstanceState) {

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == ENABLE_NETWORK_REQUEST) {
			if (!isOnline()) {
				createNetErrorDialog();
				Toast.makeText(getApplicationContext(),
				    context.getString(R.string.needs_internet_alert_title),
				    Toast.LENGTH_LONG).show();
			} else if (getRegistrationId(context).isEmpty()) {
				gcmRegisterTask = registerInBackground();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Check device for Play Services APK.
		checkPlayServices();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	private AsyncTask<Void, Void, Boolean> registerInBackground() {
		return new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {

				boolean has_registered = false;

				long backoff = Constants.BACKOFF_MILLI_SECONDS + Constants.random.nextInt(1000);

				for (int i = 1; i <= Constants.MAX_ATTEMPTS; i++) {
					Log.d(Constants.TAG, "Attempt #" + i + " to register");

					try {
						if (gcm == null) {
							gcm = GoogleCloudMessaging.getInstance(context);
						}
						regid = gcm.register(context.getString(R.string.sender_id));

						/* persist the regID */
						storeRegistrationId(context, regid);

						has_registered = true;
					} catch (IOException ex) {
						Log.e(Constants.TAG, "Failed to register on attempt " + i + ":" + ex);
						if (i == Constants.MAX_ATTEMPTS) {
							break;
						}
						try {
							Log.d(Constants.TAG, "Sleeping for " + backoff + " ms before retry");
							Thread.sleep(backoff);
						} catch (InterruptedException e1) {
							// Activity finished before we complete - exit.
							Log.d(Constants.TAG, "Thread interrupted: abort remaining retries!");
							Thread.currentThread().interrupt();
							has_registered = false;
						}
						// increase backoff exponentially
						backoff *= 2;
					}
				}

				return has_registered;
			}

			@Override
			protected void onPostExecute(Boolean msg) {
				if (!msg)
					Toast.makeText(getApplicationContext(), "GCM registration failed!",
					    Toast.LENGTH_LONG).show();
				else
					Toast.makeText(getApplicationContext(), "GCM registration succeded!",
					    Toast.LENGTH_LONG).show();

			}
		}.execute(null, null, null);
	}

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
				    PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(Constants.TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getSharedPreferences(
		    MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		String registrationId = prefs.getString(Constants.PROPERTY_GCM_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(Constants.TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
		    Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(Constants.TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	public boolean isRegistered() {
		final SharedPreferences prefs = getSharedPreferences(
		    MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		return prefs.getBoolean(PROPERTY_IS_REGISTERED, false);
	}

	public boolean isOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	private boolean isDriver() {
		final SharedPreferences prefs = getSharedPreferences(
		    MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		return prefs.getBoolean(PROPERTY_IS_DRIVER, false);
	}

	private void storeRegistrationId(Context context, String regid) {
		final SharedPreferences prefs = getSharedPreferences(
		    MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		int appVersion = getAppVersion(context);

		Log.i(Constants.TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Constants.PROPERTY_GCM_REG_ID, regid);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	private void storeRegistrationLicense(String license) {
		final SharedPreferences prefs = getSharedPreferences(
		    MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PROPERTY_IS_REGISTERED, true);
		editor.putBoolean(PROPERTY_IS_DRIVER, true);
		editor.putString(Constants.PROPERTY_LICENSE_PLATE, license);
		editor.commit();
	}

	public String getRegistrationLicense() {
		final SharedPreferences prefs = getSharedPreferences(
		    MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

		String license_plate = prefs
		    .getString(Constants.PROPERTY_LICENSE_PLATE, "");
		boolean is_registered = prefs.getBoolean(PROPERTY_IS_REGISTERED, false);
		boolean is_driver = prefs.getBoolean(PROPERTY_IS_DRIVER, true);
		if (!is_registered || !is_driver)
			license_plate = "";
		return license_plate;
	}

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
			    context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	public static class StuckDialogFragment extends DialogFragment {

		MainActivity mActivity = null;

		@Override
		public void onAttach(Activity activity) {
			// TODO Auto-generated method stub
			super.onAttach(activity);
			mActivity = (MainActivity) activity;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

			LayoutInflater inflater = getActivity().getLayoutInflater();
			View stuckDialogLayout = inflater.inflate(R.layout.dialog_stuck, null);

			final EditText editText = (EditText) stuckDialogLayout
			    .findViewById(R.id.editText_offender_license);
			editText.setOnEditorActionListener(mActivity.new UpperCaseEditText());

			builder
			    .setView(stuckDialogLayout)
			    .setTitle(R.string.stuck_dialog_title)
			    .setPositiveButton(R.string.notify,
			        new DialogInterface.OnClickListener() {
				        @Override
				        public void onClick(DialogInterface dialog, int id) {

					        new AsyncTask<Void, Void, Boolean>() {
						        @Override
						        protected Boolean doInBackground(Void... params) {

							        Bundle data = new Bundle();
							        final String license_plate = mActivity
							            .getRegistrationLicense();
							        data.putString(Constants.PROPERTY_OFFENDED_LICENSE_PLATE,
							            license_plate);
							        final String offender_license_plate = editText.getText()
							            .toString();
							        data.putString(Constants.PROPERTY_OFFENDER_LICENSE_PLATE,
							            offender_license_plate);
							        data.putString(Constants.PROPERTY_MESSAGE_TYPE,
							            Constants.LABEL_NOTIFY_MESSAGE);
							        data.putString(Constants.PROPERTY_OFFENSE_TIMESTAMP,
							            Constants.iso8601Format.format(new Date()));

							        long backoff = Constants.BACKOFF_MILLI_SECONDS
							            + Constants.random.nextInt(1000);
							        final String msgId = Constants.LABEL_NOTIFY_MESSAGE
							            + mActivity.msgId.incrementAndGet();

							        for (int i = 1; i <= Constants.MAX_ATTEMPTS; i++) {

								        Log.d(Constants.TAG, "Attempt #" + i + " to send msgId: " + msgId);

								        try {
									        mActivity.gcm.send(
									            mActivity.context.getString(R.string.sender_id)
									                + "@gcm.googleapis.com", msgId, data);

									        return true;
								        } catch (IOException e) {
									        Log.e(Constants.TAG, "Failed to send on attempt " + i + ":" + e);
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
								        Toast.makeText(mActivity.getApplicationContext(),
								            "Message sent", Toast.LENGTH_LONG).show();
							        } else {
								        Toast.makeText(mActivity.getApplicationContext(),
								            "Message failed to send!", Toast.LENGTH_LONG)
								            .show();
							        }
						        }
					        }.execute(null, null, null);

				        }
			        })
			    .setNegativeButton(R.string.cancel,
			        new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
					        StuckDialogFragment.this.getDialog().cancel();
				        }
			        });

			// Create the AlertDialog object and return it
			return builder.create();
		}
	}

	public class UpperCaseEditText implements EditText.OnEditorActionListener {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			EditText editText = (EditText) v;
			if (actionId == EditorInfo.IME_ACTION_SEARCH
			    || actionId == EditorInfo.IME_ACTION_DONE
			    || event.getAction() == KeyEvent.ACTION_DOWN
			    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				if (event == null || !event.isShiftPressed()) {
					final String original_string = editText.getText().toString()
					    .toUpperCase(Locale.US);
					final String new_string = original_string.replaceAll("\\s+", "")
					    .toUpperCase(Locale.US);
					editText.setText(new_string);
				}
			}
			return false; // pass on to other listeners.
		}
	}

	public class RegisterListener implements View.OnClickListener {

		private final View registerFragmentView;
		private AtomicBoolean registerRequestSent;

		public RegisterListener(View registerFragmentView) {
			this.registerFragmentView = registerFragmentView;
			registerRequestSent = new AtomicBoolean(false);
		}

		@Override
		public void onClick(View v) {

			if (this.registerRequestSent.compareAndSet(false, true)) {

				// wait for gcm Register Task
				new AsyncTask<Void, Void, Boolean>() {

					RegisterListener register_this = RegisterListener.this;

					@Override
					protected Boolean doInBackground(Void... params) {

						if (gcmRegisterTask != null) {
							try {
								/* wait for gcm request to finish */
								gcmRegisterTask.get();
							} catch (InterruptedException | ExecutionException e) {
								Log.i(Constants.TAG, "Registration failed.");
								return false;
							}
						}

						EditText editTextButton = (EditText) registerFragmentView
						    .findViewById(R.id.editText_license_plate);

						final String license_plate = editTextButton.getText().toString();

						if (license_plate.isEmpty())
							return false;

						Bundle data = new Bundle();
						data.putString(Constants.PROPERTY_LICENSE_PLATE, license_plate);
						if (MainActivity.this.isRegistered()) {
							data.putString(Constants.PROPERTY_MESSAGE_TYPE,
							    Constants.LABEL_CHANGEDRIVER_MESSAGE);
						} else {
							data.putString(Constants.PROPERTY_MESSAGE_TYPE,
							    Constants.LABEL_REGISTER_MESSAGE);
						}

						long backoff = Constants.BACKOFF_MILLI_SECONDS + Constants.random.nextInt(1000);
						final String msgIdString = Constants.LABEL_REGISTER_MESSAGE
						    + msgId.incrementAndGet();

						for (int i = 1; i <= Constants.MAX_ATTEMPTS; i++) {
							Log.d(Constants.TAG, "Attempt #" + i + " to send msgId: " + msgIdString);

							try {
								/* send registration id to backend */
								gcm.send(context.getString(R.string.sender_id)
								    + "@gcm.googleapis.com", Constants.LABEL_REGISTER_MESSAGE
								    + msgIdString, data);

								storeRegistrationLicense(license_plate);

								return true;
							} catch (IOException e) {
								Log.e(Constants.TAG, "Failed to send on attempt " + i + ":" + e);
								if (i == Constants.MAX_ATTEMPTS) {
									break;
								}
								try {
									Log.d(Constants.TAG, "Sleeping for " + backoff + " ms before retry");
									Thread.sleep(backoff);
								} catch (InterruptedException e1) {
									// Activity finished before we complete - exit.
									Log.d(Constants.TAG, "Thread interrupted: abort remaining retries!");
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
							Toast.makeText(getApplicationContext(),
							    "Car " + getRegistrationLicense() + " registered!",
							    Toast.LENGTH_LONG).show();

							// close Registration fragment
							// getSupportFragmentManager().popBackStack("tag2", 0); TODO :
							// label popBackStack
							getSupportFragmentManager().popBackStack(null, 0);
						} else if (getApplicationContext() != null) {
							// Toast.makeText(getApplicationContext(),
							// "Error registering!", Toast.LENGTH_LONG).show();

							createRegFailedDialog();
						}

						register_this.registerRequestSent.set(false);
					}
				}.execute(null, null, null);

			}
		}
	}

	public class SendMessageListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {

			new AsyncTask<Void, Void, Boolean>() {
				@Override
				protected Boolean doInBackground(Void... params) {

					String personal_reg_id = getRegistrationId(context);
					// send echo
					return ServerUtilities.send(context, personal_reg_id);
				}

				@Override
				protected void onPostExecute(Boolean msg) {
					if (msg) {
						Toast.makeText(getApplicationContext(), "Message sent",
						    Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getApplicationContext(), "Message failed to send!",
						    Toast.LENGTH_LONG).show();
					}
				}
			}.execute(null, null, null);
		}
	}

	public class INoDriverListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {

			new AsyncTask<Void, Void, Boolean>() {
				@Override
				protected Boolean doInBackground(Void... params) {

					Bundle data = new Bundle();
					data.putString(Constants.PROPERTY_MESSAGE_TYPE,
					    Constants.LABEL_NODRIVER_MESSAGE);

					long backoff = Constants.BACKOFF_MILLI_SECONDS + Constants.random.nextInt(1000);

					for (int i = 1; i <= Constants.MAX_ATTEMPTS; i++) {
						Log.d(Constants.TAG, "Attempt #" + i + " to register");

						try {
							/* send registration id to backend */
							gcm.send(context.getString(R.string.sender_id)
							    + "@gcm.googleapis.com", Constants.LABEL_NODRIVER_MESSAGE
							    + msgId.incrementAndGet(), data);

							return true;
						} catch (IOException e) {
							Log.e(Constants.TAG, "Failed to register on attempt " + i + ":" + e);
							if (i == Constants.MAX_ATTEMPTS) {
								break;
							}
							try {
								Log.d(Constants.TAG, "Sleeping for " + backoff + " ms before retry");
								Thread.sleep(backoff);
							} catch (InterruptedException e1) {
								// Activity finished before we complete - exit.
								Log.d(Constants.TAG, "Thread interrupted: abort remaining retries!");
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
						Toast.makeText(getApplicationContext(), "Message sent",
						    Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getApplicationContext(), "Message failed to send!",
						    Toast.LENGTH_LONG).show();
					}
				}
			}.execute(null, null, null);

			// close Registration fragment
			final SharedPreferences prefs = getSharedPreferences(
			    MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PROPERTY_IS_REGISTERED, false);
			editor.putBoolean(PROPERTY_IS_DRIVER, false);
			editor.commit();

			getSupportFragmentManager().popBackStack();
		}
	}

	protected void createRegFailedDialog() {

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
		    MainActivity.this);

		alertDialogBuilder.setTitle(context
		    .getString(R.string.failed_to_register_alert_title));

		alertDialogBuilder
		    .setMessage(
		        context.getString(R.string.failed_to_register_alert_message))
		    .setCancelable(false)
		    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
				    MainActivity.this.finish();
			    }
		    });

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	protected void createNetErrorDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder
		    .setMessage(context.getString(R.string.needs_internet_alert_message))
		    .setTitle(context.getString(R.string.needs_internet_alert_title))
		    .setCancelable(false)
		    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
				    Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
				    startActivityForResult(i, ENABLE_NETWORK_REQUEST);
			    }
		    }).setNeutralButton("Try again", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
				    /* this wil be overwritten */
			    }
		    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
				    MainActivity.this.finish();
			    }
		    });

		final AlertDialog alert = builder.create();
		alert.show();

		Button neutralButton = alert.getButton(DialogInterface.BUTTON_NEUTRAL);
		neutralButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View onClick) {
				/* this wil be overwritten */
				if (isOnline()) {
					if (getRegistrationId(context).isEmpty())
						gcmRegisterTask = registerInBackground();
					alert.dismiss();
				}
			}
		});
	}

}
