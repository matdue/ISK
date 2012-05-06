package de.matdue.isk;

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.IskDatabase;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;

public abstract class IskActivity extends Activity {
	
	private Menu optionsMenu;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		optionsMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}
	
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
	
	public void setRefreshActionItemState(boolean refreshing) {
		if (optionsMenu == null) {
            return;
        }

        MenuItem refreshItem = optionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
	}

}
