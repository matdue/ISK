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

import java.math.BigDecimal;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import de.matdue.isk.eve.EveApi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends IskActivity {

	// Dialogs
	private static final int DIALOG_WELCOME = 0;
	
	private BroadcastReceiver eveApiUpdaterReceiver;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Spinner spinner = (Spinner) findViewById(R.id.main_pilot);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				PilotData selectedPilot = (PilotData) parent.getItemAtPosition(position);
				getPreferences()
					.edit()
					.putString("startCharacterID", selectedPilot.characterId)
					.apply();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				getPreferences()
					.edit()
					.remove("startCharacterID")
					.apply();
			}
		});
        
        List<PilotData> pilots = new ArrayList<PilotData>();
        
        // Sort by character name
 		final Collator collator = Collator.getInstance();
 		Collections.sort(pilots, new Comparator<PilotData>() {
 			@Override
 			public int compare(PilotData lhs, PilotData rhs) {
 				return collator.compare(lhs.name, rhs.name);
 			}
 		});
 		
 		PilotAdapter adapter = new PilotAdapter(this, pilots);
        spinner.setAdapter(adapter);
        refreshPilots();
        
        // Make sure update service to be called regularly
     	WakefulIntentService.scheduleAlarms(new EveApiUpdaterListener(), getApplicationContext(), false);
     		
        showWelcomeDialog();
    }
    
    public void gotoWallet(View view) {
    	String characterID = getPreferences().getString("startCharacterID", null);
		if (characterID != null) {
			Intent intent = new Intent(this, WalletActivity.class);
			intent.putExtra("characterID", characterID);
			startActivity(intent);
		}
    }
    
    public void gotoMarketOrders(View view) {
    	String characterID = getPreferences().getString("startCharacterID", null);
		if (characterID != null) {
			Intent intent = new Intent(this, MarketOrderActivity.class);
			intent.putExtra("characterID", characterID);
			startActivity(intent);
		}
    }
    
    public void gotoPilots(View view) {
    	startActivity(new Intent(this, PilotsActivity.class));
    }
    
    @Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		Dialog dialog = null;
		switch (id) {
		case DIALOG_WELCOME:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
				.setMessage(R.string.main_dialog_welcome)
				.setNeutralButton(R.string.main_dialog_welcome_close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences preferences = getPreferences();
						preferences
							.edit()
							.putBoolean("welcomed", true)
							.commit();
						dialog.dismiss();
					}
				});
			dialog = builder.create();
			break;
		}
		
		return dialog;
	}
    
    /**
	 * Show a Welcome! dialog
	 */
	private void showWelcomeDialog() {
		SharedPreferences preferences = getPreferences();
		boolean welcomed = preferences.getBoolean("welcomed", false);
		if (!welcomed) {
			showDialog(DIALOG_WELCOME);
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_options, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			refreshCurrentCharacter();
			return true;
		
		case R.id.main_optmenu_eveaccess:
			startActivity(new Intent(this, EveAccessActivity.class));
			return true;
			
		case R.id.main_optmenu_preferences:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
			
		case R.id.main_optmenu_info:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		refreshPilots();
		
		// Register broadcast receiver: If character data has been updated in background,
		// show latest data immediately
		IntentFilter filter = new IntentFilter(EveApiUpdaterService.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        eveApiUpdaterReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				// Character has been updated in background
				// => Update view now, if the current character has been updated
				String characterId = intent.getStringExtra("characterId");
				String currentCharacterID = getPreferences().getString("startCharacterID", null);
				if (currentCharacterID != null && currentCharacterID.equals(characterId)) {
					refreshPilots();
				}
				
				String errorMessage = intent.getStringExtra("error");
				if (errorMessage != null) {
					String message = getResources().getString(R.string.main_refresh_error, errorMessage);
					Toast.makeText(context, message, Toast.LENGTH_LONG).show();
				}
				
				// Stop progress bar
				setRefreshActionItemState(false);
			}
		};
        registerReceiver(eveApiUpdaterReceiver, filter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(eveApiUpdaterReceiver);
		
		// Stop progress bar
		setRefreshActionItemState(false);
	}
	
	
	private void refreshPilots() {
		new AsyncTask<Void, Void, List<PilotData>>() {
			
			String characterID;
			
			@Override
			protected List<PilotData> doInBackground(Void... params) {
				// Get active pilot
				characterID = getPreferences().getString("startCharacterID", null);
				
				// Load all pilots and their balance
				List<PilotData> pilots = new ArrayList<PilotData>();
				Cursor pilotsCursor = getDatabase().queryCharactersAndBalance();
				while (pilotsCursor.moveToNext()) {
					PilotData pilotData = new PilotData();
					pilotData.characterId = pilotsCursor.getString(0);
					pilotData.name = pilotsCursor.getString(1);
					pilotData.balance = BigDecimal.ZERO;
					String sBalance = pilotsCursor.getString(2);
					if (sBalance != null && sBalance.length() > 0) {
						pilotData.balance = new BigDecimal(sBalance);
					}
					
					pilots.add(pilotData);
				}
				pilotsCursor.close();
				
				// Sort by character name
		 		final Collator collator = Collator.getInstance();
		 		Collections.sort(pilots, new Comparator<PilotData>() {
		 			@Override
		 			public int compare(PilotData lhs, PilotData rhs) {
		 				return collator.compare(lhs.name, rhs.name);
		 			}
		 		});
		 		
				return pilots;
			}
			
			@Override
			protected void onPostExecute(List<PilotData> result) {
		 		// Find index of active pilot
		 		int activePilot = 0;
		 		for (int i = 0; i < result.size(); ++i) {
		 			if (result.get(i).characterId.equals(characterID)) {
		 				activePilot = i;
		 				break;
		 			}
		 		}
		 		
		 		// Update spinner and its adapter
		 		Spinner spinner = (Spinner) findViewById(R.id.main_pilot);
		 		PilotAdapter adapter = (PilotAdapter) spinner.getAdapter();
		 		adapter.refresh(result);
		 		adapter.notifyDataSetChanged();
		 		spinner.setSelection(activePilot);
			}
		}.execute();
	}
	
	/**
	 * Refreshes current character
	 */
	private void refreshCurrentCharacter() {
		String currentCharacterID = getPreferences().getString("startCharacterID", null);
		if (currentCharacterID == null) {
			return;
		}
		
		// Force refresh of current character
		Intent msgIntent = new Intent(this, EveApiUpdaterService.class);
		msgIntent.putExtra("characterId", currentCharacterID);
		msgIntent.putExtra("force", true);
		WakefulIntentService.sendWakefulWork(this, msgIntent);
		
		// Show refresh animation
		setRefreshActionItemState(true);
	}
	
	
	private static class PilotData {
		public String characterId;
		public String name;
		public BigDecimal balance;
	}
	
	private class PilotAdapter extends BaseAdapter {
		
		List<PilotData> pilots;
		LayoutInflater inflater;
		NumberFormat balanceFormatter = NumberFormat.getInstance();
		
		public PilotAdapter(Context context, List<PilotData> pilots) {
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.pilots = pilots;
			
			balanceFormatter.setMinimumFractionDigits(2);
			balanceFormatter.setMaximumFractionDigits(2);
		}

		@Override
		public int getCount() {
			return pilots.size();
		}

		@Override
		public Object getItem(int position) {
			return pilots.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getPilotView(position, convertView, parent, R.layout.main_pilot);
		}
		
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getPilotView(position, convertView, parent, R.layout.main_pilot_spinner_item);
		}
		
		public void refresh(List<PilotData> pilots) {
			this.pilots = pilots;
		}
		
		private View getPilotView(int position, View convertView, ViewGroup parent, int resource) {
			View view = convertView;
			if (view == null) {
				view = inflater.inflate(resource, parent, false);
			}
			
			ImageView pilotImage = (ImageView) view.findViewById(R.id.main_pilot_image);
			TextView pilotName = (TextView) view.findViewById(R.id.main_pilot_name);
			TextView pilotBalance = (TextView) view.findViewById(R.id.main_pilot_balance);
			
			PilotData pilot = pilots.get(position);
			getBitmapManager().setImageBitmap(pilotImage, EveApi.getCharacterUrl(pilot.characterId, 128));
			pilotName.setText(pilot.name);
			if (pilotBalance != null) {
				if (pilot.balance != null) {
					String sBalance = balanceFormatter.format(pilot.balance) + " ISK";
					pilotBalance.setText(sBalance);
				} else {
					pilotBalance.setText("");
				}
			}
			
			return view;
		}
		
	}
	
}