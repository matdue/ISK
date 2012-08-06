/**
 * Copyright 2012 Matthias Düsterhöft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.matdue.isk;

import java.util.Arrays;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.view.MenuItem;
import android.view.Window;

public class PreferencesActivity extends IskActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		getFragmentManager().beginTransaction()
	        .replace(android.R.id.content, new SettingsFragment())
	        .commit();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
            
        default:
    		return super.onOptionsItemSelected(item);
		}
	}
	
	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
		
		private String previousUpdateInterval;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.preferences);
			
			// Prefill some values
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			previousUpdateInterval = preferences.getString("updateInterval", null);
			updateUpdateInterval(preferences);
			updateRingtone(preferences);
			updateVibration(preferences);
		}

		
		@Override
		public void onResume() {
			super.onResume();
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			preferences.registerOnSharedPreferenceChangeListener(this);

			// RingtonePreference does not fire onSharedPreferenceChanged()...
			RingtonePreference ringtonePreference = (RingtonePreference) findPreference("ringtone");
			ringtonePreference.setOnPreferenceChangeListener(this);
		}
		
		@Override
		public void onPause() {
			super.onPause();
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if ("updateInterval".equals(key)) {
				updateUpdateInterval(sharedPreferences);
			} else if ("vibration".equals(key)) {
				updateVibration(sharedPreferences);
			}
		}
		
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if ("ringtone".equals(preference.getKey())) {
				updateRingtone(newValue.toString());
			}
			return true;
		}

		private void updateUpdateInterval(SharedPreferences preferences) {
			String updateInterval = preferences.getString("updateInterval", null);
			if (updateInterval != null) {
				// Update summary
				String[] updateIntervalValues = getResources().getStringArray(R.array.update_interval_values);
				int idxUpdateInterval = Arrays.asList(updateIntervalValues).indexOf(updateInterval);
				if (idxUpdateInterval != -1) {
					String txtUpdateInterval = getResources().getStringArray(R.array.update_interval)[idxUpdateInterval];
					findPreference("updateInterval").setSummary(txtUpdateInterval);
				}
				
				// Restart service on change
				if (!updateInterval.equals(previousUpdateInterval)) {
					WakefulIntentService.cancelAlarms(getActivity().getApplicationContext());
					WakefulIntentService.scheduleAlarms(new EveApiUpdaterListener(), getActivity().getApplicationContext(), true);
					previousUpdateInterval = updateInterval;
				}
			}
		}

		private void updateVibration(SharedPreferences preferences) {
			String vibration = preferences.getString("vibration", null);
			if (vibration != null) {
				String[] vibrationValues = getResources().getStringArray(R.array.notification_vibration_values);
				int idxVibration = Arrays.asList(vibrationValues).indexOf(vibration);
				if (idxVibration != -1) {
					String txtVibration = getResources().getStringArray(R.array.notification_vibration)[idxVibration];
					findPreference("vibration").setSummary(txtVibration);
				}
			}
		}

		private void updateRingtone(SharedPreferences preferences) {
			String ringtone = preferences.getString("ringtone", "");
			updateRingtone(ringtone);
		}

		private void updateRingtone(String ringtoneUri) {
			String ringtoneName = null;
			if (!"".equals(ringtoneUri)) {
				Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(ringtoneUri));
				if (ringtone != null) {
					ringtoneName = ringtone.getTitle(getActivity());
				}
			} 
			if (ringtoneName == null) {
				ringtoneName = getString(R.string.preferences_ringtone_silent);
			}
			findPreference("ringtone").setSummary(ringtoneName);
		}
	}
	
}
