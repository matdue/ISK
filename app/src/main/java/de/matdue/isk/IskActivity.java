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

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.EveDatabase;
import de.matdue.isk.database.IskDatabase;
import android.app.Activity;
import android.content.Context;
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
	
	public EveDatabase getEveDatabase() {
		IskApplication iskApp = (IskApplication) getApplication();
		return iskApp.getEveDatabase();
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
	
	/**
	 * Looks up a string from a string array. An integer array gives a hint about the correct position.
	 * 
	 * Example:
	 * <string-array name="greetings">
	 *   <item>Hello</item>
	 *   <item>World</item>
	 * </string-array>
	 * <integer-array name="greetingsIdx">
	 *   <item>100</item>
	 *   <item>200</item>
	 * </integer-array>
	 * 
	 * getIndexedResourceString(greetings, greetingsIdx, 200, 100) will return "World"
	 * getIndexedResourceString(greetings, greetingsIdx, 999, 100) will return "Hello"
	 * 
	 * @param stringArrayId Resource ID of string array
	 * @param indexArrayId Resource ID of integer array
	 * @param key String to look up
	 * @param defaultValueIdx Default string
	 * @return String from resource
	 */
	public String getIndexedResourceString(int stringArrayId, int indexArrayId, int key, int defaultValueIdx) {
		return getIndexedResourceString(this, stringArrayId, indexArrayId, key, defaultValueIdx);
	}
	
	public static String getIndexedResourceString(Context context, int stringArrayId, int indexArrayId, int key, int defaultValueIdx) {
		int[] keys = context.getResources().getIntArray(indexArrayId);
		String[] values = context.getResources().getStringArray(stringArrayId);
		int valueIdx = Arrays.binarySearch(keys, key);
		return (valueIdx < values.length) ? values[valueIdx] : values[defaultValueIdx];
	}
	
}
