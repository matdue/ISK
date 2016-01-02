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
import java.util.Arrays;
import java.util.Comparator;

import de.matdue.isk.account.AccountAuthenticator;
import de.matdue.isk.account.AuthenticatorActivity;
import de.matdue.isk.account.SyncAdapter;
import de.matdue.isk.database.ApiAccount;
import de.matdue.isk.database.Balance;
import de.matdue.isk.eve.EveApi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends IskActivity implements NavigationView.OnNavigationItemSelectedListener {

	public static final BigDecimal ONE_MILLION = new BigDecimal(1000000);
	public static final BigDecimal ONE_BILLION = new BigDecimal(1000000000);

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

		String startCharacterName = getPreferences().getString("startCharacterName", null);
		if (startCharacterName != null) {
			switchToAccount(startCharacterName);
		}

		// Show/hide elements for new users
		showWelcomeText();
    }

	private void showWelcomeText() {
		Account[] accounts = AccountManager.get(this).getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
		boolean hasAccounts = accounts.length != 0;
		findViewById(R.id.main_welcome).setVisibility(hasAccounts ? View.GONE : View.VISIBLE);
		findViewById(R.id.main_balance).setVisibility(hasAccounts ? View.VISIBLE : View.GONE);
		findViewById(R.id.main_navigation_buttons).setVisibility(hasAccounts ? View.VISIBLE : View.GONE);

		if (!hasAccounts) {
			// Make sure no account is selected
			switchToAccount(null);
		} else {
			// Make sure an account is selected; select the first one if none was selected
			String startCharacterName = getPreferences().getString("startCharacterName", null);
			if (startCharacterName == null) {
				switchToAccount(accounts[0].name);
			} else {
				// Check if current account is still existing
				boolean startCharacterIsKnown = false;
				for (Account account : accounts) {
					if (startCharacterName.equals(account.name)) {
						startCharacterIsKnown = true;
						break;
					}
				}

				if (!startCharacterIsKnown) {
					switchToAccount(accounts[0].name);
				}
			}
		}
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
				String[] authorities = {SyncAdapter.CONTENT_AUTHORITY};
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
					switchToAccount(menuItem.getTitle().toString());
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

		// The following menu modification destroys the ripple effect for an unknown reason.
		Menu navigationViewMenu = navigationView.getMenu();
		navigationViewMenu.setGroupVisible(R.id.navdrawer_menu, !drawerShowAccounts);
		navigationViewMenu.setGroupVisible(R.id.navdrawer_menu_accounts, drawerShowAccounts);
		navigationViewMenu.setGroupVisible(R.id.navdrawer_menu_accountmgmt, drawerShowAccounts);

		// Add accounts
		navigationViewMenu.removeGroup(R.id.navdrawer_menu_accounts);
		if (drawerShowAccounts) {
			AccountManager accountManager = AccountManager.get(this);
			Account[] accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

			// Sort by name
			final Collator collator = Collator.getInstance();
			Arrays.sort(accounts, new Comparator<Account>() {
				@Override
				public int compare(Account x, Account y) {
					return collator.compare(x.name, y.name);
				}
			});

			// Add menu entry with a general person icon.
			// We cannot display the real character image as there is a filter on each menu item
			// which would result in a grey rectangular only.
			int order = Menu.FIRST;
			for (Account account : accounts) {
				navigationViewMenu.add(R.id.navdrawer_menu_accounts, Menu.NONE, order++, account.name)
						.setIcon(R.drawable.ic_person_white_24dp);
			}
		}
	}

	private static class AccountData {
		ApiAccount apiAccount;
		Balance balance;
		Bitmap pilotImage;
	}

	private void switchToAccount(String name) {
		View navigationHeader = navigationView.getHeaderView(0);

		// Display name and reset all other fields
		TextView nameTextView = (TextView) navigationHeader.findViewById(R.id.pilot_name);
		nameTextView.setText(name);

		final TextView corporationTextView = (TextView) navigationHeader.findViewById(R.id.pilot_corporation);
		corporationTextView.setText(null);

		final TextView allianceTextView = (TextView) navigationHeader.findViewById(R.id.pilot_alliance);
		allianceTextView.setText(null);
		allianceTextView.setVisibility(View.GONE);

		final ImageView pilotImageView = (ImageView) navigationHeader.findViewById(R.id.profile_image);
		pilotImageView.setImageResource(R.drawable.ic_main_pilots);

		// Load corporation name etc. from database
		new AsyncTask<String, Void, AccountData>() {
			@Override
			protected AccountData doInBackground(String... params) {
				String characterName = params[0];
				if (characterName == null) {
					return null;
				}

				ApiAccount apiAccount = getDatabase().queryApiAccount(characterName);
				if (apiAccount == null) {
					return null;
				}

				Balance balance = getDatabase().queryBalance(apiAccount.characterId);

				AccountData accountData = new AccountData();
				accountData.apiAccount = apiAccount;
				accountData.balance = balance;
				accountData.pilotImage = getBitmapManager().getImage(EveApi.getCharacterUrl(apiAccount.characterId, calculateResolution(pilotImageView)));

				return accountData;
			}

			private int calculateResolution(ImageView imageView) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					// ImageView.getMaxWidth() is not available before Jelly Bean
					int maxWidth = imageView.getMaxWidth();
					return maxWidth < 128 ? 128 : 256;
				} else {
					return 128;
				}
			}

			@Override
			protected void onPostExecute(AccountData accountData) {
				if (accountData == null) {
					return;
				}

				corporationTextView.setText(accountData.apiAccount.corporationName);
				allianceTextView.setText(accountData.apiAccount.allianceName);
				allianceTextView.setVisibility(TextUtils.isEmpty(accountData.apiAccount.allianceName) ? View.GONE : View.VISIBLE);
				if (accountData.pilotImage != null) {
					pilotImageView.setImageBitmap(accountData.pilotImage);
				}

				ImageView mainPilotImage = (ImageView) findViewById(R.id.main_pilot_image);
				if (accountData.pilotImage != null) {
					mainPilotImage.setImageBitmap(accountData.pilotImage);
				} else {
					mainPilotImage.setImageResource(R.drawable.ic_main_pilots);
				}

				TextView mainPilotName = (TextView) findViewById(R.id.main_pilot_name);
				mainPilotName.setText(accountData.apiAccount.characterName);

				if (accountData.balance != null) {
					NumberFormat balanceFormatter = NumberFormat.getInstance();
					balanceFormatter.setMinimumFractionDigits(2);
					balanceFormatter.setMaximumFractionDigits(2);
					String balance = getString(R.string.main_balance_value, balanceFormatter.format(accountData.balance.balance));

					TextView mainBalance = (TextView) findViewById(R.id.main_pilot_balance);
					mainBalance.setText(balance);

					mainBalance = (TextView) findViewById(R.id.main_pilot_balance_human);
					if (accountData.balance.balance.compareTo(ONE_MILLION) >= 0) {
						if (accountData.balance.balance.compareTo(ONE_BILLION) < 0) {
							// Between 1 million and 1 billion
							balance = balanceFormatter.format(accountData.balance.balance.divide(ONE_MILLION, 2, BigDecimal.ROUND_HALF_UP));
							mainBalance.setText(getString(R.string.main_balance_million, balance));
						} else {
							// 1 Billion or more
							balance = balanceFormatter.format(accountData.balance.balance.divide(ONE_BILLION, 2, BigDecimal.ROUND_HALF_UP));
							mainBalance.setText(getString(R.string.main_balance_billion, balance));
						}
						mainBalance.setVisibility(View.VISIBLE);
					} else {
						mainBalance.setVisibility(View.GONE);
					}
				}

				if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					reportFullyDrawn();
				}
			}
		}.execute(name);

		getPreferences()
				.edit()
				.putString("startCharacterName", name)
				.apply();
	}

	public void gotoWallet(View view) {
		String characterName = getPreferences().getString("startCharacterName", null);
		if (characterName == null) {
			return;
		}

		new AsyncTask<String, Void, ApiAccount>() {
			@Override
			protected ApiAccount doInBackground(String... params) {
				ApiAccount apiAccount = getDatabase().queryApiAccount(params[0]);
				return apiAccount;
			}

			@Override
			protected void onPostExecute(ApiAccount apiAccount) {
				if (apiAccount != null) {
					WalletActivity.navigate(MainActivity.this, apiAccount.characterId);
				}
			}
		}.execute(characterName);
    }
    
    public void gotoMarketOrders(View view) {
		String characterName = getPreferences().getString("startCharacterName", null);
		if (characterName == null) {
			return;
		}

		new AsyncTask<String, Void, ApiAccount>() {
			@Override
			protected ApiAccount doInBackground(String... params) {
				ApiAccount apiAccount = getDatabase().queryApiAccount(params[0]);
				return apiAccount;
			}

			@Override
			protected void onPostExecute(ApiAccount apiAccount) {
				if (apiAccount != null) {
					MarketOrderActivity.navigate(MainActivity.this, apiAccount.characterId, apiAccount.characterName);
				}
			}
		}.execute(characterName);
    }

	public void gotoSetupPilot(View view) {
		AuthenticatorActivity.navigate(this);
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
		case R.id.menu_refresh:
			refreshCurrentCharacter();
			return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Register broadcast receiver: If character data has been updated in background,
		// show latest data immediately
		IntentFilter filter = new IntentFilter(SyncAdapter.SYNC_FINISHED_BROADCAST);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        eveApiUpdaterReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				// Character has been updated in background
				// => Update view now, if the current character has been updated
				String characterName = intent.getStringExtra("account");
				Log.d("MainActivity", "Got SYNC_FINISHED broadcast for account " + characterName);
				String currentCharacterName = getPreferences().getString("startCharacterName", null);
				if (currentCharacterName != null && currentCharacterName.equals(characterName)) {
					switchToAccount(characterName);
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

		showWelcomeText();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(eveApiUpdaterReceiver);
		
		// Stop progress bar
		setRefreshActionItemState(false);

		showWelcomeText();
	}
	
	/**
	 * Refreshes current character
	 */
	private void refreshCurrentCharacter() {
		String characterName = getPreferences().getString("startCharacterName", null);
		if (characterName == null) {
			return;
		}

		// Look up corresponding account
		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
		for (Account account : accounts) {
			if (!account.name.equals(characterName)) {
				continue;
			}

			Bundle extras = new Bundle();
			extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
			extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			ContentResolver.requestSync(account, SyncAdapter.CONTENT_AUTHORITY, extras);

			// Show refresh animation
			setRefreshActionItemState(true);

			return;
		}
	}

}