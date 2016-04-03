/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.example.android.sunshine.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public final String LOG_TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
    }

    // Registers a shared preference change listener that gets notified when preferences change
    @Override
    protected void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    // Unregisters a shared preference change listener
    @Override
    protected void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Set the preference summaries
        setPreferenceSummary(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private void setPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();
        String key = preference.getKey();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
    }

    // This gets called before the preference is changed
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {

        Log.d(LOG_TAG, "Preference " + preference.getKey() + " changed to " + String.valueOf(value));

        if (preference.getKey().equals(getString(R.string.pref_location_key))) {
            setPreferenceSummary(preference, String.format(
                    getString(R.string.pref_location_unknown_description),
                    String.valueOf(value)));
        } else {
            setPreferenceSummary(preference, value);
        }
        return true;
    }

    // This gets called after the preference is changed, which is important because we
    // start our synchronization here
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if ( key.equals(getString(R.string.pref_location_key)) ) {

            try {
                Log.d(LOG_TAG, "SharedPreference " + key + " changed to " + String.valueOf(sharedPreferences.getInt(key, -1)));
            } catch (ClassCastException e) {
                Log.d(LOG_TAG, "SharedPreference " + key + " changed to " + sharedPreferences.getString(key, "?"));
            }
            Utility.resetLocationStatus(this);
            SunshineSyncAdapter.syncImmediately(this);
        } else if ( key.equals(getString(R.string.sp_server_status))) {
            @SunshineSyncAdapter.LocationStatus int newStatus = Utility.getLocationStatus(this);
            Log.d(LOG_TAG, "LocationStatus changed to " + String.valueOf(newStatus));

            /**
             * FIXME: this can be cleaned up. The answer showed the value being obtained using
             * haivng this check done in the setPreferenceSummary() instead. That function
             * takes an Object, avoiding the need to handle a type error
             */
            String locationValue;
            try {
                int location = sharedPreferences.getInt(getString(R.string.pref_location_key), -1);
                locationValue = String.valueOf(location);
            } catch (ClassCastException e) {
                locationValue = sharedPreferences.getString(getString(R.string.pref_location_key), "?");
            }

            // If the server status is good, use the location value as the text
            String statusText = String.format("(unimplemented status state: {})", newStatus);
            if (newStatus == SunshineSyncAdapter.LOCATION_STATUS_OK) {
                statusText = locationValue;
            } else if (newStatus == SunshineSyncAdapter.LOCATION_STATUS_INVALID) {
                statusText = String.format(getString(R.string.pref_location_error_description), locationValue);
            }
            setPreferenceSummary(findPreference(getString(R.string.pref_location_key)), statusText);

        } else if ( key.equals(getString(R.string.pref_units_key)) ) {
            // units have changed. update lists of weather entries accordingly
            getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
