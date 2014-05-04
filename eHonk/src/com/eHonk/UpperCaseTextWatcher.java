package com.eHonk;

import java.util.Locale;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class UpperCaseTextWatcher implements TextWatcher {

	boolean filtered = false;

	@Override
	public void afterTextChanged(Editable s) {
		if (!filtered) {
			EditText editText = (EditText) s;
			filtered = true;
			final String original_string = editText.getText()
					.toString().toUpperCase(Locale.US);
			final String new_string = original_string.replaceAll(
					"\\s+", "").toUpperCase(Locale.US);
			editText.setText(new_string);
			editText.setSelection(new_string.length());
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before,
			int count) {
	}

}
