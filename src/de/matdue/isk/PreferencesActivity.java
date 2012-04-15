package de.matdue.isk;

import java.util.Arrays;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	private String previousUpdateInterval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		addPreferencesFromResource(R.xml.preferences);
		
		// Prefill some values
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		previousUpdateInterval = preferences.getString("updateInterval", null);
		updateUpdateInterval(preferences);
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
	
	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if ("updateInterval".equals(key)) {
			updateUpdateInterval(sharedPreferences);
		}
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
				WakefulIntentService.cancelAlarms(getApplicationContext());
//				WakefulIntentService.scheduleAlarms(new EveApiUpdaterListener(), getApplicationContext(), true);
				previousUpdateInterval = updateInterval;
			}
		}
	}

}
