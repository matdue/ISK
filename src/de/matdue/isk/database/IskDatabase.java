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
package de.matdue.isk.database;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class IskDatabase extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "isk.db";
	private static final int DATABASE_VERSION = 1;
	
	public IskDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(EveApiCacheTable.SQL_CREATE);
		db.execSQL(EveApiHistoryTable.SQL_CREATE);
		db.execSQL(ApiKeyTable.SQL_CREATE);
		db.execSQL(CharacterTable.SQL_CREATE);
		db.execSQL(BalanceTable.SQL_CREATE);
		db.execSQL(WalletTable.SQL_CREATE);
		db.execSQL(WalletTable.IDX_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(WalletTable.IDX_DROP);
		db.execSQL(WalletTable.SQL_DROP);
		db.execSQL(BalanceTable.SQL_DROP);
		db.execSQL(CharacterTable.SQL_DROP);
		db.execSQL(ApiKeyTable.SQL_DROP);
		db.execSQL(EveApiHistoryTable.SQL_DROP);
		db.execSQL(EveApiCacheTable.SQL_DROP);
		onCreate(db);
	}

	public Cursor getApiKeyCursor() {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(ApiKeyTable.TABLE_NAME,
					new String[] {
					ApiKeyTable.ID,
					ApiKeyTable.KEY
				},
				null,  // where
				null,  // where arguments
				null,  // group by
				null,  // having
				null); // order by
			
			return cursor;
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "getApiKeyCursor", e);
		}
		
		return null;
	}
	
	public String getApiKeyID(long id) {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = null;
			try {
				cursor = db.query(ApiKeyTable.TABLE_NAME, 
						new String[] { ApiKeyTable.KEY }, 
						ApiKeyTable.ID + "=?",      // where
						new String[] { Long.toString(id) },  // where arguments
						null,  // group by
						null,  // having
						null); // order by
				if (cursor.moveToNext()) {
					return cursor.getString(0);
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "getApiKeyID", e);
		}
		
		return null;
	}
	
	public Cursor getCharacterCursor(long apiId) {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(CharacterTable.TABLE_NAME,
					new String[] {
					CharacterTable.ID,
					CharacterTable.NAME,
					CharacterTable.SELECTED,
					CharacterTable.CHARACTER_ID
				},
				CharacterTable.API_ID + "=?",  // where
				new String[] { 
					Long.toString(apiId) 
				},  // where arguments
				null,  // group by
				null,  // having
				CharacterTable.NAME); // order by
			
			return cursor;
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "getCharacterCursor", e);
		}
		
		return null;
	}
	
	public void setCharacterSelection(long id, boolean checked) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(CharacterTable.SELECTED, checked);
			db.update(CharacterTable.TABLE_NAME, 
					values, 
					CharacterTable.ID + "=?", 
					new String[] { Long.toString(id) });
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "setCharacterSelection", e);
		}
	}
	
	public void deleteApiKey(long id) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			db.delete(CharacterTable.TABLE_NAME,
					CharacterTable.API_ID + "=?",
					new String[] { Long.toString(id) });
			
			db.delete(ApiKeyTable.TABLE_NAME, 
					ApiKeyTable.ID + "=?", 
					new String[] { Long.toString(id) });
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "deleteApiKey", e);
		}
	}
	
	public void insertApiKey(ApiKey apiKey, List<Character> characters) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			InsertHelper apiKeyInsertHelper = new InsertHelper(db, ApiKeyTable.TABLE_NAME);
			InsertHelper characterInsertHelper = new InsertHelper(db, CharacterTable.TABLE_NAME);
			try {
				db.beginTransaction();
				
				ContentValues values = new ContentValues();
				values.put(ApiKeyTable.KEY, apiKey.key);
				values.put(ApiKeyTable.CODE, apiKey.code);
				long apiKeyId = apiKeyInsertHelper.insert(values);
				
				for (Character character : characters) {
					values = new ContentValues();
					values.put(CharacterTable.API_ID, apiKeyId);
					values.put(CharacterTable.CHARACTER_ID, character.characterId);
					values.put(CharacterTable.NAME, character.name);
					values.put(CharacterTable.CORPORATION_ID, character.corporationId);
					values.put(CharacterTable.CORPORATION_NAME, character.corporationName);
					values.put(CharacterTable.SELECTED, character.selected);
					characterInsertHelper.insert(values);
				}
				
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
				characterInsertHelper.close();
				apiKeyInsertHelper.close();
			}
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "insertApiKey", e);
		}
	}
	
	public List<Character> queryAllCharacters() {
		List<Character> result = new ArrayList<Character>();
		
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(true, 
					CharacterTable.TABLE_NAME, 
					new String[] { CharacterTable.NAME, CharacterTable.CHARACTER_ID }, 
					CharacterTable.SELECTED + "=?", 
					new String[] { "1" }, 
					null, 
					null, 
					null, 
					null);
			while (cursor.moveToNext()) {
				Character character = new Character();
				character.name = cursor.getString(0);
				character.characterId = cursor.getString(1);
				
				result.add(character);
			}
			cursor.close();
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "queryAllCharacters", e);
		}
		
		return result;
	}
	
	public Character queryCharacter(String characterId) {
		Character result = null;
		
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(CharacterTable.TABLE_NAME, 
					new String[] { CharacterTable.NAME }, 
					CharacterTable.CHARACTER_ID + "=?", 
					new String[] { characterId }, 
					null, 
					null, 
					null, 
					null);
			if (cursor.moveToNext()) {
				result = new Character();
				result.characterId = characterId;
				result.name = cursor.getString(0);
			}
			cursor.close();
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "queryCharacter", e);
		}
		
		return result;
	}
	
	public Balance queryBalance(String characterId) {
		Balance result = null;
		
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(BalanceTable.TABLE_NAME, 
					new String[] { BalanceTable.BALANCE }, 
					BalanceTable.CHARACTER_ID + "=?", 
					new String[] { characterId }, 
					null, 
					null, 
					null, 
					null);
			if (cursor.moveToNext()) {
				result = new Balance();
				result.characterId = characterId;
				result.balance = new BigDecimal(cursor.getString(0));
			}
			cursor.close();
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "queryBalance", e);
		}
		
		return result;
	}
	
	public void storeBalance(Balance balance) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			InsertHelper balanceInsertHelper = new InsertHelper(db, BalanceTable.TABLE_NAME);
			try {
				db.beginTransaction();
				
				db.delete(BalanceTable.TABLE_NAME, 
						BalanceTable.CHARACTER_ID + "=?", 
						new String[] { balance.characterId });
				
				ContentValues values = new ContentValues();
				values.put(BalanceTable.CHARACTER_ID, balance.characterId);
				values.put(BalanceTable.BALANCE, balance.balance.toString());
				balanceInsertHelper.insert(values);
				
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
				balanceInsertHelper.close();
			}
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "storeBalance", e);
		}
	}
	
	public Cursor queryCharactersAndBalance() {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(CharacterTable.TABLE_NAME + " LEFT JOIN " + BalanceTable.TABLE_NAME + " ON " + CharacterTable.TABLE_NAME + "." + CharacterTable.CHARACTER_ID + "=" + BalanceTable.TABLE_NAME + "." + BalanceTable.CHARACTER_ID,
					new String[] {
					CharacterTable.TABLE_NAME + "." + CharacterTable.CHARACTER_ID,
					CharacterTable.TABLE_NAME + "." + CharacterTable.NAME,
					BalanceTable.TABLE_NAME + "." + BalanceTable.BALANCE
				},
				CharacterTable.TABLE_NAME + "." + CharacterTable.SELECTED + "=?",  // where
				new String[] { "1" },  // where arguments
				null,  // group by
				null,  // having
				null); // order by
			
			return cursor;
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "queryCharactersAndBalance", e);
		}
		
		return null;
	}

	public ApiKey queryApiKey(String characterId) {
		ApiKey result = null;
		
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(ApiKeyTable.TABLE_NAME + " INNER JOIN " + CharacterTable.TABLE_NAME + " ON " + ApiKeyTable.TABLE_NAME + "." + ApiKeyTable.ID + " = " + CharacterTable.TABLE_NAME + "." + CharacterTable.API_ID, 
					new String[] { ApiKeyTable.TABLE_NAME + "." + ApiKeyTable.KEY, ApiKeyTable.TABLE_NAME + "." + ApiKeyTable.CODE }, 
					CharacterTable.TABLE_NAME + "." + CharacterTable.CHARACTER_ID + "=?", 
					new String [] { characterId }, 
					null, 
					null, 
					null);
			if (cursor.moveToNext()) {
				result = new ApiKey();
				result.key = cursor.getString(0);
				result.code = cursor.getString(1);
			}
			cursor.close();
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "queryApiKey", e);
		}
		
		return result;
	}
	
	public boolean isEveApiCacheValid(String key) {
		boolean result = false;
		
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(EveApiCacheTable.TABLE_NAME, 
					new String[] { EveApiCacheTable.CACHED_UNTIL }, 
					EveApiCacheTable.KEY + "=?", 
					new String[] { key }, 
					null, 
					null, 
					null);
			if (cursor.moveToNext()) {
				long cachedUntil = cursor.getLong(0);
				result = cachedUntil > System.currentTimeMillis();
			}
			cursor.close();
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "isEveApiCacheValid", e);
		}
		
		return result;
	}
	
	public void storeEveApiCache(String key, Date cachedUntil) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			InsertHelper insertHelper = new InsertHelper(db, EveApiCacheTable.TABLE_NAME);
			try {
				db.beginTransaction();
				
				db.delete(EveApiCacheTable.TABLE_NAME, 
						EveApiCacheTable.KEY + "=?", 
						new String[] { key });
				
				ContentValues values = new ContentValues();
				values.put(EveApiCacheTable.KEY, key);
				values.put(EveApiCacheTable.CACHED_UNTIL, cachedUntil.getTime());
				insertHelper.insert(values);
				
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
				insertHelper.close();
			}
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "storeEveApiCache", e);
		}
	}
	
	public void cleanupEveApiHistory() {
		try {
			SQLiteDatabase db = getWritableDatabase();
			
			long aWeekAgo = System.currentTimeMillis() - 7l*24l*60l*60l*1000l;
			db.delete(EveApiHistoryTable.TABLE_NAME, 
					EveApiHistoryTable.TIMESTAMP + "<?", 
					new String[] { Long.toString(aWeekAgo) });
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "cleanupEveApiHistory", e);
		}
	}
	
	public void storeEveApiHistory(String url, String keyID, String result) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			InsertHelper insertHelper = new InsertHelper(db, EveApiHistoryTable.TABLE_NAME);
			try {
				ContentValues values = new ContentValues();
				values.put(EveApiHistoryTable.URL, url);
				values.put(EveApiHistoryTable.KEY_ID, keyID);
				values.put(EveApiHistoryTable.RESULT, result);
				values.put(EveApiHistoryTable.TIMESTAMP, System.currentTimeMillis());
				insertHelper.insert(values);
			} finally {
				insertHelper.close();
			}
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "storeEveApiHistory", e);
		}
	}
	
	public Cursor getEveApiHistoryCursor() {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(EveApiHistoryTable.TABLE_NAME,
					new String[] {
					"rowid _id",
					EveApiHistoryTable.TIMESTAMP,
					EveApiHistoryTable.URL,
					EveApiHistoryTable.KEY_ID,
					EveApiHistoryTable.RESULT
				},
				null,  // where
				null,  // where arguments
				null,  // group by
				null,  // having
				EveApiHistoryTable.TIMESTAMP + " desc"); // order by
			
			return cursor;
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "getEveApiHistoryCursor", e);
		}
		
		return null;
	}

	public void storeEveWallet(String characterId, List<Wallet> wallets) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			InsertHelper insertHelper = new InsertHelper(db, WalletTable.TABLE_NAME);
			try {
				db.beginTransaction();
				
				db.delete(WalletTable.TABLE_NAME, 
						WalletTable.CHARACTER_ID + "=?", 
						new String[] { characterId });
				
				for (Wallet wallet : wallets) {
					ContentValues values = new ContentValues();
					values.put(WalletTable.CHARACTER_ID, characterId);
					values.put(WalletTable.DATE, wallet.date.getTime());
					values.put(WalletTable.REFTYPEID, wallet.refTypeID);
					values.put(WalletTable.OWNERNAME1, wallet.ownerName1);
					values.put(WalletTable.OWNERNAME2, wallet.ownerName2);
					values.put(WalletTable.AMOUNT, wallet.amount.toString());
					values.put(WalletTable.TAX_AMOUNT, wallet.taxAmount.toString());
					values.put(WalletTable.QUANTITY, wallet.quantity);
					values.put(WalletTable.TYPE_NAME, wallet.typeName);
					values.put(WalletTable.TYPE_ID, wallet.typeID);
					values.put(WalletTable.PRICE, wallet.price != null ? wallet.price.toString() : null);
					values.put(WalletTable.CLIENT_NAME, wallet.clientName);
					values.put(WalletTable.STATION_NAME, wallet.stationName);
					values.put(WalletTable.TRANSACTION_TYPE, wallet.transactionType);
					values.put(WalletTable.TRANSACTION_FOR, wallet.transactionFor);
					insertHelper.insert(values);
				}
				
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
				insertHelper.close();
			}
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "storeEveWallet", e);
		}
	}
	
	public Cursor getEveWallet(String characterId) {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(WalletTable.TABLE_NAME,
					new String[] {
					"rowid _id",
					WalletTable.DATE,
					WalletTable.REFTYPEID,
					WalletTable.OWNERNAME1,
					WalletTable.OWNERNAME2,
					WalletTable.OWNERNAME2,
					WalletTable.AMOUNT,
					WalletTable.TAX_AMOUNT,
					WalletTable.QUANTITY,
					WalletTable.TYPE_NAME,
					WalletTable.PRICE,
					WalletTable.CLIENT_NAME,
					WalletTable.STATION_NAME,
					WalletTable.TRANSACTION_TYPE,
					WalletTable.TRANSACTION_FOR,
					WalletTable.TYPE_ID
				},
				WalletTable.CHARACTER_ID + "=?",  // where
				new String[] { characterId },  // where arguments
				null,  // group by
				null,  // having
				WalletTable.DATE + " desc"); // order by
			
			return cursor;
		} catch (SQLiteException e) {
			Log.e("IskDatabase", "getEveWallet", e);
		}
		return null;
	}
	
}
