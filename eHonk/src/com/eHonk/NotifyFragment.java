package com.eHonk;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class NotifyFragment extends Fragment {

	MainActivity mActivity = null;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity)activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);

		TextView textViewRegisterInfo = (TextView) rootView.findViewById(R.id.register_info);
		TextView textViewLicenseInfo = (TextView) rootView.findViewById(R.id.license_info);
		TextView textViewChangeLicense = (TextView) rootView.findViewById(R.id.change_license);

		textViewChangeLicense.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				android.support.v4.app.FragmentManager fm = mActivity.getSupportFragmentManager();
				android.support.v4.app.FragmentTransaction ft2 = fm
						.beginTransaction();
				ft2.replace(R.id.container, new RegisterFragment(), "tag2");
				ft2.addToBackStack(null);
				ft2.commit();
			}
		});

		final String license = MainActivity.getRegistrationLicense(mActivity);

		if (license.isEmpty()) {
			textViewRegisterInfo.setText(mActivity.getString(R.string.no_vehicule));
			textViewLicenseInfo.setVisibility(View.GONE);
			textViewChangeLicense.setText(mActivity.getString(R.string.register_button));
		} else {
			textViewLicenseInfo.setText(license);
		}

		Button stuckButton = (Button) rootView.findViewById(R.id.stuck_button);
		stuckButton
				.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						new MainActivity.StuckDialogFragment().show(mActivity.getSupportFragmentManager(), "stuck");
					}
				});
		
		Button notificationsButton = (Button) rootView.findViewById(R.id.notification_list_button);
		notificationsButton
				.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						android.support.v4.app.FragmentManager fm = mActivity.getSupportFragmentManager();
						android.support.v4.app.FragmentTransaction ft3 = fm
						    .beginTransaction();
						NotificationListFragment fragment = new NotificationListFragment();
						ft3.replace(R.id.container, fragment, "fragment_tag3");
						ft3.addToBackStack("tag3");
						ft3.commit();
					}
				});

		return rootView;
	}
}
