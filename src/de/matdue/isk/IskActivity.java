package de.matdue.isk;

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.IskDatabase;
import android.app.Activity;
import android.content.SharedPreferences;

public abstract class IskActivity extends Activity {
	
	public IskDatabase getDatabase() {
		IskApplication iskApp = (IskApplication) getApplication();
		return iskApp.getIskDatabase();
	}
	
	public BitmapManager getBitmapManager() {
		IskApplication iskApp = (IskApplication) getApplication();
		return iskApp.getBitmapManager();
	}
	
	public SharedPreferences getPreferences() {
		SharedPreferences preferences = getSharedPreferences("de.matdue.isk", MODE_PRIVATE);
		return preferences;
	}

}
