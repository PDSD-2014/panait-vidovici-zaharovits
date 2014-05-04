package com.eHonk;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RegisterFragment extends Fragment {

	MainActivity mActivity = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_register,
				container, false);
		
		mActivity = (MainActivity) getActivity();

		final EditText editText = (EditText) rootView
				.findViewById(R.id.editText_license_plate);

		if (mActivity.isRegistered()) {
			TextView registerMenuTextView = (TextView) rootView
					.findViewById(R.id.textView_register_menu);
			registerMenuTextView.setText(mActivity.context
					.getString(R.string.change_license_title));

			final String old_license_plate = mActivity
					.getRegistrationLicense();
			editText.setText(old_license_plate);
		}

		editText.setOnEditorActionListener(mActivity.new UpperCaseEditText());

		Button registerButton = (Button) rootView
				.findViewById(R.id.button_register);
		registerButton.setOnClickListener(mActivity.new RegisterListener(
				rootView));

		TextView notNowTextView = (TextView) rootView
				.findViewById(R.id.textView_not_now);
		notNowTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// close Registration fragment
				mActivity.getSupportFragmentManager().popBackStack();
			}
		});

		TextView iNoDriverTextView = (TextView) rootView
				.findViewById(R.id.textView_I_no_driver);
		iNoDriverTextView
				.setOnClickListener(mActivity.new INoDriverListener());

		return rootView;
	}
}
