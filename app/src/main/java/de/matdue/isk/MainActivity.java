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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import de.matdue.isk.account.AccountAuthenticator;
import de.matdue.isk.account.AuthenticatorActivity;
import de.matdue.isk.database.ApiAccount;
import de.matdue.isk.eve.EveApi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends IskActivity implements NavigationView.OnNavigationItemSelectedListener {

	private BroadcastReceiver eveApiUpdaterReceiver;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private boolean drawerShowAccounts;
	private NavigationView navigationView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main3);

		// Init toolbar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Init navigation drawer
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navigationView = (NavigationView) findViewById(R.id.drawer_navigation_view);
		navigationView.setNavigationItemSelectedListener(this);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				super.onDrawerSlide(drawerView, 0);  // this disables the arrow @ completed state
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, 0);  // this disables the animation
			}
		};
		drawerLayout.setDrawerListener(drawerToggle);


		/*getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.main2);

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerLayout.setStatusBarBackground(R.color.theme_primary_dark);
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name);
		drawerLayout.setDrawerListener(drawerToggle);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		*/
        /*super.onCreate(savedInstanceState);
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
     	*/
        showWelcomeDialog();
    }

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem menuItem) {
		drawerLayout.closeDrawers();
		switch (menuItem.getItemId()) {
			case R.id.navdrawer_history:
				EveAccessActivity.navigate(this);
				return true;

			case R.id.navdrawer_preferences:
				PreferencesActivity.navigate(this);
				return true;

			case R.id.navdrawer_help:
				AboutActivity.navigate(this);
				return true;

			case R.id.navdrawer_account_manage:
				String[] authorities = {"de.matdue.isk.content.provider"};
				Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
				intent.putExtra(Settings.EXTRA_AUTHORITIES, authorities);
				startActivity(intent);
				toggleNavAccountView(false);
				return true;

			case R.id.navdrawer_account_add:
				AuthenticatorActivity.navigate(this);
				toggleNavAccountView(false);
				return true;

			case Menu.NONE:
				if (menuItem.getGroupId() == R.id.navdrawer_menu_accounts) {
					switchToAccount(menuItem.getTitle());
					toggleNavAccountView(false);
					return true;
				}
		}

		return false;
	}

	public void toggleNavAccountView(View view) {
		toggleNavAccountView(!drawerShowAccounts);
	}

	public void toggleNavAccountView(boolean showAccounts) {
		if (drawerShowAccounts == showAccounts) {
			return;
		}

		drawerShowAccounts = showAccounts;
		ImageView expandIndicator = ((ImageView) findViewById(R.id.expand_account_box_indicator));
		expandIndicator.setImageResource(drawerShowAccounts ? R.drawable.ic_arrow_drop_up_white_24dp : R.drawable.ic_arrow_drop_down_white_24dp);

		Menu navigationViewMenu = navigationView.getMenu();
		navigationViewMenu.setGroupVisible(R.id.navdrawer_menu, !drawerShowAccounts);
		navigationViewMenu.setGroupVisible(R.id.navdrawer_menu_accounts, drawerShowAccounts);
		navigationViewMenu.setGroupVisible(R.id.navdrawer_menu_accountmgmt, drawerShowAccounts);

		navigationViewMenu.removeGroup(R.id.navdrawer_menu_accounts);
		if (drawerShowAccounts) {
			AccountManager accountManager = AccountManager.get(this);
			Account[] accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
			final Collator collator = Collator.getInstance();
			Arrays.sort(accounts, new Comparator<Account>() {
				@Override
				public int compare(Account x, Account y) {
					return collator.compare(x.name, y.name);
				}
			});

			int order = Menu.FIRST;
			for (Account account : accounts) {
				navigationViewMenu.add(R.id.navdrawer_menu_accounts, Menu.NONE, order++, account.name)
						.setIcon(R.drawable.ic_person_white_24dp);
			}
		}
	}

	private void switchToAccount(CharSequence name) {
		final TextView nameTextView = (TextView) findViewById(R.id.profile_name_text);
		nameTextView.setVisibility(View.INVISIBLE);
		final TextView corpTextView = (TextView) findViewById(R.id.profile_email_text);
		corpTextView.setVisibility(View.INVISIBLE);
		final ImageView profileImage = (ImageView) findViewById(R.id.profile_image);
		profileImage.setImageResource(R.drawable.ic_main_pilots);

		new AsyncTask<CharSequence, Void, ApiAccount>() {
			@Override
			protected ApiAccount doInBackground(CharSequence... params) {
				CharSequence name = params[0];
				List<ApiAccount> allApiAccounts = getDatabase().queryAllApiAccounts();

				// Character matching?
				for (ApiAccount apiAccount : allApiAccounts) {
					if (name.equals(apiAccount.characterName)) {
						return apiAccount;
					}
				}

				// Corporation matching?
				for (ApiAccount apiAccount : allApiAccounts) {
					if (apiAccount.characterName == null && name.equals(apiAccount.corporationName)) {
						return apiAccount;
					}
				}

				return null;
			}

			@Override
			protected void onPostExecute(ApiAccount apiAccount) {
				if (apiAccount == null) {
					// TODO: Was tun?
					return;
				}

				if (apiAccount.characterName == null) {
					// Corporation
					nameTextView.setText(apiAccount.corporationName);
					nameTextView.setVisibility(View.VISIBLE);

					getBitmapManager().setImageBitmap(profileImage, EveApi.getCorporationUrl(apiAccount.corporationId, 128), null, null);
					profileImage.setVisibility(View.VISIBLE);
				} else {
					// Character
					nameTextView.setText(apiAccount.characterName);
					nameTextView.setVisibility(View.VISIBLE);

					corpTextView.setText(apiAccount.corporationName);
					corpTextView.setVisibility(View.VISIBLE);

					getBitmapManager().setImageBitmap(profileImage, EveApi.getCharacterUrl(apiAccount.characterId, 128), null, null);
				}
			}
		}.execute(name);
	}

	public void selectAccount(View view) {
		Log.d("MainActivity", "Select account " + view.getTag());
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
    	/*startActivity(new Intent(this, PilotsActivity.class));*/
		//startActivity(new Intent(this, AuthenticatorActivity.class));

		final AccountManager accountManager = AccountManager.get(this);
		final Account[] accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
		for (final Account account : accounts) {
			if ("Quaax".equals(account.name)) {
				/*accountManager.getAuthToken(account, AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER, null, this, new AccountManagerCallback<Bundle>() {
					@Override
					public void run(AccountManagerFuture<Bundle> future) {
						try {
							Bundle bundle = future.getResult();
							String authtoken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
							Log.d("MainActivity", authtoken);

							accountManager.invalidateAuthToken(account.type, authtoken);
						} catch (OperationCanceledException e) {
							Toast.makeText(MainActivity.this, R.string.account_authentication_aborted, Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							Toast.makeText(MainActivity.this, R.string.account_authentication_failed, Toast.LENGTH_LONG).show();
						}
					}
				}, null);*/
				/*accountManager.getAuthToken(account, AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER, null, false, new AccountManagerCallback<Bundle>() {
					@Override
					public void run(AccountManagerFuture<Bundle> future) {
						try {
							Bundle bundle = future.getResult();
							String authtoken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
							if (authtoken != null) {
								Log.d("MainActivity", authtoken);
								accountManager.invalidateAuthToken(account.type, authtoken);
							} else {
								Log.d("MainActivity", "Got no auth token");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, null);*/
			}
		}

		accountManager.getAccountsByTypeAndFeatures(AccountAuthenticator.ACCOUNT_TYPE, new String[]{"apiCharacter"}, new AccountManagerCallback<Account[]>() {
			@Override
			public void run(AccountManagerFuture<Account[]> future) {
				try {
					Account[] aa = future.getResult();
					for (Account a : aa) {
						Log.d("MainActivity", "Char: " + a.name);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, null);
		accountManager.getAccountsByTypeAndFeatures(AccountAuthenticator.ACCOUNT_TYPE, new String[]{"apiCorporation"}, new AccountManagerCallback<Account[]>() {
			@Override
			public void run(AccountManagerFuture<Account[]> future) {
				try {
					Account[] aa = future.getResult();
					for (Account a : aa) {
						Log.d("MainActivity", "Corp: " + a.name);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, null);
	}
    
    /**
	 * Show a Welcome! dialog
	 */
	private void showWelcomeDialog() {
		SharedPreferences preferences = getPreferences();
		boolean welcomed = preferences.getBoolean("welcomed", false);
		if (!welcomed) {
			FragmentManager fm = getFragmentManager();
			if (fm.findFragmentByTag("WelcomeDialog") == null) {
				FragmentTransaction ft = fm.beginTransaction().addToBackStack(null);
				WelcomeDialogFragment fragment = WelcomeDialogFragment.newInstance();
				fragment.show(ft, "WelcomeDialog");
			}
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_options, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
		/*case android.R.id.home:
			drawerLayout.openDrawer(GravityCompat.START);
			return true;*/

		case R.id.menu_refresh:
			refreshCurrentCharacter();
			return true;
		
		/*case R.id.main_optmenu_eveaccess:
			startActivity(new Intent(this, EveAccessActivity.class));
			return true;
			
		case R.id.main_optmenu_preferences:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
			
		case R.id.main_optmenu_info:
			//startActivity(new Intent(this, AboutActivity.class));
			AboutActivity.navigate(this);
			return true;*/
			
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
		/*new AsyncTask<Void, Void, List<PilotData>>() {
			@Override
			protected List<PilotData> doInBackground(Void... params) {
				List<ApiAccount> apiAccounts = getDatabase().queryAllApiAccounts();
				ArrayList<PilotData> pilots = new ArrayList<>();
				for (ApiAccount apiAccount : apiAccounts) {
					PilotData pilot = new PilotData();
					pilot.balance = BigDecimal.ZERO;
					pilot.name = apiAccount.characterName != null ? apiAccount.characterName : apiAccount.corporationName;
					pilot.characterId = apiAccount.characterId != null ? apiAccount.characterId : apiAccount.corporationId;
					pilot.corporation = apiAccount.corporationName != null;

					pilots.add(pilot);
				}

				// Sort by name, characters first
				final Collator collator = Collator.getInstance();
				Collections.sort(pilots, new Comparator<PilotData>() {
					@Override
					public int compare(PilotData x, PilotData y) {
						if (!x.corporation && y.corporation) {
							return -1;
						} else if (x.corporation && !y.corporation) {
							return 1;
						}

						return collator.compare(x.name, y.name);
					}
				});

				return pilots;
			}

			@Override
			protected void onPostExecute(List<PilotData> result) {
				// Update spinner and its adapter
				Spinner spinner = (Spinner) findViewById(R.id.main_pilot);
				PilotAdapter adapter = (PilotAdapter) spinner.getAdapter();
				adapter.refresh(result);
				adapter.notifyDataSetChanged();
			}
		}.execute();
		return;*/

		/*AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
		for (Account account : accounts) {
			String apiAccountType = accountManager.getUserData(account, "api");
			if (apiAccountType == null) {
				continue;
			}

			PilotData pilotData = new PilotData();
			pilotData.characterId = accountManager.getUserData(account, "characterID");
			pilotData.name = account.name;
			pilotData.corporation = apiAccountType.equals(AccountAuthenticator.AUTHTOKEN_TYPE_API_CORPORATION);
			pilotData.balance = BigDecimal.ZERO;
			pilots.add(pilotData);
		}

		// Find index of active pilot
		int activePilot = 0;
		for (int i = 0; i < pilots.size(); ++i) {
			if (pilots.get(i).characterId.equals(activeCharacterID)) {
				activePilot = i;
				break;
			}
		}

		// Update spinner and its adapter
		Spinner spinner = (Spinner) findViewById(R.id.main_pilot);
		PilotAdapter adapter = (PilotAdapter) spinner.getAdapter();
		adapter.refresh(pilots);
		adapter.notifyDataSetChanged();
		spinner.setSelection(activePilot);*/
	}

	private void refreshPilotsOld() {
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
		/*String currentCharacterID = getPreferences().getString("startCharacterID", null);
		if (currentCharacterID == null) {
			return;
		}
		
		// Force refresh of current character
		Intent msgIntent = new Intent(this, EveApiUpdaterService.class);
		msgIntent.putExtra("characterId", currentCharacterID);
		msgIntent.putExtra("force", true);
		WakefulIntentService.sendWakefulWork(this, msgIntent);*/
		
		// Show refresh animation
		setRefreshActionItemState(true);
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				android.os.SystemClock.sleep(3000);
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				setRefreshActionItemState(false);
			}
		}.execute();
	}
	
	
	private static class PilotData {
		public String characterId;
		public String name;
		public BigDecimal balance;
		public boolean corporation;
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
			getBitmapManager().setImageBitmap(pilotImage, (pilot.corporation ? EveApi.getCorporationUrl(pilot.characterId, 128) : EveApi.getCharacterUrl(pilot.characterId, 128)), null, null);
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
	
	public static class WelcomeDialogFragment extends DialogFragment {
		
		public static WelcomeDialogFragment newInstance() {
			WelcomeDialogFragment instance = new WelcomeDialogFragment();
			return instance;
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder
				.setMessage(R.string.main_dialog_welcome)
				.setNeutralButton(R.string.main_dialog_welcome_close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences preferences = ((IskActivity)getActivity()).getPreferences();
						preferences
							.edit()
							.putBoolean("welcomed", true)
							.apply();
						dialog.dismiss();
					}
				});
			return builder.create();		
		}
		
	}
	
}