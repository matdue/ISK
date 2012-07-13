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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import de.matdue.isk.database.ApiKey;
import de.matdue.isk.database.Balance;
import de.matdue.isk.database.EveDatabase;
import de.matdue.isk.database.OrderWatch;
import de.matdue.isk.database.Wallet;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.eve.AccountBalance;
import de.matdue.isk.eve.CacheInformation;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCache;
import de.matdue.isk.eve.MarketOrder;
import de.matdue.isk.eve.WalletJournal;

public class EveApiUpdaterService extends WakefulIntentService {
	
	public static final String ACTION_RESP = "de.matdue.isk.EVE_API_UPDATER_FINISHED";
	
	private IskDatabase iskDatabase;
	private EveDatabase eveDatabase;
	private EveApi eveApi;

	public EveApiUpdaterService() {
		super("de.matdue.isk.EveApiUpdaterService");
	}
	
	@Override
	protected void doWakefulWork(Intent intent) {
		Log.d("EveApiUpdaterService", "Performing update");
		
		// Skip update if no network is available
		if (!isNetworkAvailable()) {
			sendEmptyResponseIntent();
			return;
		}
		
		boolean forcedUpdate = intent.getBooleanExtra("force", false);

		if (!forcedUpdate) {
			// Skip update if WIFI inactive, but required
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			boolean requireWifi = sharedPreferences.getBoolean("requireWifi", false);
			if (requireWifi && !isWifiConnected()) {
				sendEmptyResponseIntent();
				return;
			}
			
			// Skip update if global sync is switched off
			boolean honorGlobalSync = sharedPreferences.getBoolean("honorGlobalSync", true);
			if (honorGlobalSync && !ContentResolver.getMasterSyncAutomatically()) {
				sendEmptyResponseIntent();
				return;
			}
		}
		
		try {
			iskDatabase = new IskDatabase(this);
			eveDatabase = new EveDatabase(this);
			eveApi = new EveApi(new EveApiCacheDatabase(forcedUpdate));
			
			// If 'characterId' is given, update that specific character only
			// else update all characters
			String characterId = intent.getStringExtra("characterId");
			if (characterId != null) {
				updateCharacter(characterId);
			} else {
				updateAllCharacters();
			}
		} catch (Exception e) {
			Log.e("EveApiUpdaterService",  "Error while performing update", e);
			String message = e.getMessage();
			if (message == null) {
				message = e.toString();
			}
			sendBroadcast(new Intent(ACTION_RESP)
				.addCategory(Intent.CATEGORY_DEFAULT)
				.putExtra("error", message));
		} finally {
			iskDatabase.close();
			eveDatabase.close();
			eveApi.close();
		}
	}
	
	private void sendEmptyResponseIntent() {
		sendBroadcast(new Intent(ACTION_RESP).addCategory(Intent.CATEGORY_DEFAULT));
	}
	
	/**
	 * Network detection
	 */
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}
	
	/**
	 * WIFI detection
	 */
	private boolean isWifiConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnectedOrConnecting();
	}

	/**
	 * Update all characters
	 */
	private void updateAllCharacters() {
		List<de.matdue.isk.database.Character> characters = iskDatabase.queryAllCharacters();
		for (de.matdue.isk.database.Character character : characters) {
			updateCharacter(character.characterId);
		}
		iskDatabase.cleanupEveApiHistory();
	}
	
	/**
	 * Update a single character by querying Eve Online API and storing all data in database.
	 * 
	 * @param characterId Character's EVE id
	 */
	private void updateCharacter(String characterId) {
		updateBalance(characterId);
		updateWallet(characterId);
		updateMarketOrders(characterId);
		
		// Inform listeners about updated character
		sendBroadcast(new Intent(ACTION_RESP)
			.addCategory(Intent.CATEGORY_DEFAULT)
			.putExtra("characterId", characterId));
	}
	
	/**
	 * Update a character's balance.
	 * 
	 * @param characterId Character's EVE id
	 */
	private void updateBalance(String characterId) {
		ApiKey apiKey = iskDatabase.queryApiKey(characterId);
		if (apiKey != null) {
			AccountBalance accountBalance = eveApi.queryAccountBalance(apiKey.key, apiKey.code, characterId);
			if (accountBalance != null) {
				Balance balance = new Balance();
				balance.balance = accountBalance.balance;
				balance.characterId = characterId;
				iskDatabase.storeBalance(balance);
			}
		}
	}
	
	private void updateWallet(String characterId) {
		ApiKey apiKey = iskDatabase.queryApiKey(characterId);
		if (apiKey != null) {
			List<WalletJournal> wallet = eveApi.queryWallet(apiKey.key, apiKey.code, characterId);
			if (wallet != null) {
				// Prepare data objects
				ArrayList<Wallet> walletDatas = new ArrayList<Wallet>();
				for (WalletJournal walletJournalEntry : wallet) {
					Wallet walletData = new Wallet();
					walletData.date = walletJournalEntry.date;
					walletData.refTypeID = walletJournalEntry.refTypeID;
					walletData.ownerName1 = walletJournalEntry.ownerName1;
					walletData.ownerName2 = walletJournalEntry.ownerName2;
					walletData.amount = walletJournalEntry.amount;
					walletData.taxAmount = walletJournalEntry.taxAmount;
					
					if (walletJournalEntry.transaction != null) {
						walletData.quantity = walletJournalEntry.transaction.quantity;
						walletData.typeName = walletJournalEntry.transaction.typeName;
						walletData.typeID = walletJournalEntry.transaction.typeID;
						walletData.price = walletJournalEntry.transaction.price;
						walletData.clientName = walletJournalEntry.transaction.clientName;
						walletData.stationName = walletJournalEntry.transaction.stationName;
						walletData.transactionType = walletJournalEntry.transaction.transactionType;
						walletData.transactionFor = walletJournalEntry.transaction.transactionFor;
					}
					
					walletDatas.add(walletData);
				}
				iskDatabase.storeEveWallet(characterId, walletDatas);
			}
		}
	}
	
	private OrderWatch createOrderWatch(MarketOrder marketOrder, String characterId, SparseArray<String> itemNameCache, SparseArray<String> stationNameCache) {
		OrderWatch orderWatch = new OrderWatch();
		orderWatch.characterId = characterId;
		orderWatch.orderID = marketOrder.orderID;
		orderWatch.orderState = marketOrder.orderState;
		
		orderWatch.typeID = marketOrder.typeID;
		String typeName = itemNameCache.get(marketOrder.typeID);
		if (typeName == null) {
			typeName = eveDatabase.queryTypeName(marketOrder.typeID);
			itemNameCache.put(marketOrder.typeID, typeName);
		}
		orderWatch.typeName = typeName;
		
		orderWatch.stationID = marketOrder.stationID;
		String stationName = stationNameCache.get(marketOrder.stationID);
		if (stationName == null) {
			stationName = eveDatabase.queryStationName(marketOrder.stationID);
			stationNameCache.put(marketOrder.stationID, stationName);
		}
		orderWatch.stationName = stationName;
		
		orderWatch.price = marketOrder.price;
		orderWatch.volEntered = marketOrder.volEntered;
		orderWatch.volRemaining = marketOrder.volRemaining;
		orderWatch.fulfilled = 100 - ((marketOrder.volEntered != 0) ? (100 * marketOrder.volRemaining / marketOrder.volEntered) : 0);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(marketOrder.issued);
		calendar.add(Calendar.DATE, marketOrder.duration);
		orderWatch.expires = calendar.getTime();
		
		orderWatch.action = marketOrder.bid;
		orderWatch.status = 0;
		orderWatch.sortKey = 0;
		
		return orderWatch;
	}
	
	private void updateMarketOrders(String characterId) {
		ApiKey apiKey = iskDatabase.queryApiKey(characterId);
		if (apiKey != null) {
			List<MarketOrder> marketOrders = eveApi.queryMarketOrders(apiKey.key, apiKey.code, characterId);
			if (marketOrders != null) {
				long seqId = SystemClock.elapsedRealtime();
				
				// Load old data objects (for expired orders)
				List<OrderWatch> oldOrderWatches = iskDatabase.queryAllOrderWatches(characterId);
				
				// Prepare data objects
				ArrayList<OrderWatch> orderWatches = new ArrayList<OrderWatch>();
				
				// Copy expired orders
				for (OrderWatch oldOrderWatch : oldOrderWatches) {
					if (oldOrderWatch.orderID == 0) {
						oldOrderWatch.seqId = ++seqId;
						orderWatches.add(oldOrderWatch);
					}
				}
				
				// Cache for item name and station name to minimize database access
				SparseArray<String> itemNameCache = new SparseArray<String>();
				SparseArray<String> stationNameCache = new SparseArray<String>();
				
				for (MarketOrder marketOrder : marketOrders) {
					// Ignore market orders with a duration of 0
					// These are immediate buy/sell orders
					if (marketOrder.duration == 0) {
						continue;
					}
					
					OrderWatch orderWatch = createOrderWatch(marketOrder, characterId, itemNameCache, stationNameCache);
					orderWatch.seqId = ++seqId;

					// Look up order in 'oldOrderWatches'
					OrderWatch savedOrderWatch = null;
					for (OrderWatch oldOrderWatch : oldOrderWatches) {
						if (oldOrderWatch.orderID == orderWatch.orderID) {
							savedOrderWatch = oldOrderWatch;
							break;
						}
					}
					
					if (orderWatch.orderState == 0) {
						// Active order
						orderWatch.sortKey = 1000;
						if (savedOrderWatch != null) {
							orderWatch.status = savedOrderWatch.status;
						} else {
							// TODO: Automatically watch order if item ID is in OrderWatchItems
						}
						
						orderWatches.add(orderWatch);
					} else {
						// Inactive order: If it was watched, remember it
						if (savedOrderWatch != null) {
							orderWatch.orderID = 0;
							orderWatch.sortKey = 0;
							orderWatches.add(orderWatch);
							
							// TODO: Notify about expires/fulfilled order
						}
					}
				}
				
				// Mark missing orders as expired/fulfilled
				for (OrderWatch oldOrderWatch : oldOrderWatches) {
					if (oldOrderWatch.orderID == 0) {
						continue;
					}
					
					boolean isMissing = true;
					for (MarketOrder marketOrder : marketOrders) {
						if (oldOrderWatch.orderID == marketOrder.orderID) {
							isMissing = false;
							break;
						}
					}
					if (isMissing) {
						// Add as inactive order
						oldOrderWatch.orderID = 0;
						oldOrderWatch.sortKey = 0;
						orderWatches.add(oldOrderWatch);
						
						// TODO: Notify about expires/fulfilled order
					}
				}
				
				iskDatabase.storeOrderWatches(characterId, orderWatches);
			}
		}
	}
	
	private class EveApiCacheDatabase implements EveApiCache {
		
		private boolean forcedUpdate;
		
		public EveApiCacheDatabase(boolean forcedUpdate) {
			this.forcedUpdate = forcedUpdate;
		}

		@Override
		public boolean isCached(String key) {
			if (forcedUpdate) {
				return false;
			} else {
				return iskDatabase.isEveApiCacheValid(key);
			}
		}

		@Override
		public void cache(String key, CacheInformation cacheInformation) {
			iskDatabase.storeEveApiCache(key, cacheInformation.cachedUntil);
		}

		@Override
		public void urlAccessed(String url, String keyID, String result) {
			iskDatabase.storeEveApiHistory(url, keyID, result);
		}
		
	}
	
}
