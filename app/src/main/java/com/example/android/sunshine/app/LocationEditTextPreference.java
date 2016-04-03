package com.example.android.sunshine.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

/**
 * Created by mkazin on 4/3/2016.
 */
public class LocationEditTextPreference extends EditTextPreference {

    public final String LOG_TAG = LocationEditTextPreference.class.getSimpleName();

    private static final int DEFULT_MINIMUM_LENGTH = 3;
    private int mMinLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.d(LOG_TAG, "Parsing styled attributes...");
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.LocationEditTextPreference,
                0, 0);

        try {
            mMinLength = a.getInteger(R.styleable.LocationEditTextPreference_minLength,
                    DEFULT_MINIMUM_LENGTH);
            Log.d(LOG_TAG, String.format("Minimum length set to %d", mMinLength));
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Listen to text changes
        super.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(LOG_TAG, "afterTextChanged");
                Dialog d = getDialog();
                if (d instanceof AlertDialog) {
                    AlertDialog dialog = (AlertDialog) d;
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    positiveButton.setEnabled(s.length() >= mMinLength);
                }
            }
        });
    }

    public void setmMinLength(int minLength) {
        mMinLength = minLength;
    }
    public int getmMinLength() {
        return mMinLength;
    }
}
