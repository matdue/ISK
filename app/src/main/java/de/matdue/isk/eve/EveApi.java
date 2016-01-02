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
package de.matdue.isk.eve;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

import android.net.Uri;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import android.util.Xml.Encoding;

import javax.net.ssl.HttpsURLConnection;

public class EveApi {
	
	private static final SimpleDateFormat dateFormatter;
	private static final String AGENT = "Android de.matdue.isk";
	private static final String IMAGE_BASE = "https://imageserver.eveonline.com/";

	private EveApiCache apiCache;

	static {
		// EVE Online API always uses GMT
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public EveApi(EveApiCache apiCache) {
		this.apiCache = apiCache;
	}
	
	public static String getCharacterUrl(String characterId, int resolution) {
		return IMAGE_BASE + "Character/" + characterId + "_" + resolution + ".jpg";
	}

	public static String getCorporationUrl(String corporationId, int resolution) {
		return IMAGE_BASE + "Corporation/" + corporationId + "_" + resolution + ".png";
	}

	public static String getTypeUrl(String typeID, int resolution) {
		return IMAGE_BASE + "Type/" + typeID + "_" + resolution + ".png";
	}
	
	private boolean queryApi(ContentHandler xmlParser, String url, String keyID, String vCode) {
		return queryApi(xmlParser, url, keyID, vCode, null);
	}
	
	private boolean queryApi(ContentHandler xmlParser, String url, String keyID, String vCode, String characterID) {
		return queryApi(xmlParser, url, keyID, vCode, characterID, null, null);
	}
	
	private boolean queryApi(ContentHandler xmlParser, String url, String keyID, String vCode, String characterID, String rowCount, String fromID) {
		InputStream inputStream = null;
		HttpsURLConnection connection = null;
		
		try {
			// Create request
			Uri.Builder uriBuilder = new Uri.Builder()
					.scheme("https")
					.authority("api.eveonline.com")
					.path(url)
					.appendQueryParameter("keyID", keyID)
					.appendQueryParameter("vCode", vCode);

			// Prepare parameters
			if (characterID != null) {
				uriBuilder.appendQueryParameter("characterID", characterID);
			}
			if (rowCount != null) {
				uriBuilder.appendQueryParameter("rowCount", rowCount);
			}
			if (fromID != null) {
				uriBuilder.appendQueryParameter("fromID", fromID);
			}

			// Submit request
			URL requestURL = new URL(uriBuilder.build().toString());
			connection = (HttpsURLConnection) requestURL.openConnection();
			// For debugging requests:
			// connection = (HttpsURLConnection) requestURL.openConnection(HttpsURLConnectionUtils.buildProxy("10.0.2.2", 8888));
			// HttpsURLConnectionUtils.trustAllCertificates(connection);
			connection.setRequestProperty("User-Agent", AGENT);
			connection.setDoOutput(true);

			// Record access
			int statusCode = connection.getResponseCode();
			String reasonPhrase = connection.getResponseMessage();
			apiCache.urlAccessed(requestURL.getPath(), keyID, statusCode + " " + reasonPhrase);
			
			if (statusCode != HttpsURLConnection.HTTP_OK) {
				Log.e(EveApi.class.toString(), "API returned with code " + statusCode);
				return false;
			}

			inputStream = new BufferedInputStream(connection.getInputStream());
			Xml.parse(inputStream, Encoding.UTF_8, xmlParser);
			
			return true;
		} catch (Exception e) {
			String message = e.getMessage();
			apiCache.urlAccessed(url, keyID, message != null ? message : e.toString());
			Log.e(EveApi.class.toString(), "Error in API communication", e);
		} finally {
			if (connection != null) {
				try {
					connection.disconnect();
				} catch (Exception e) {
					// Ignore error while closing, there's nothing we could do
				}

			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) {
					// Ignore error while closing, there's nothing we could do
				}
			}
		}
		
		return false;
	}
	
	public Account validateKey(String keyID, String vCode) {
		final String URL = "/account/APIKeyInfo.xml.aspx";
		
		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(URL, keyID, vCode);
		if (apiCache.isCached(cacheKey)) {
			return null;
		}
		
		Account result = new Account();
		CacheInformation cacheInformation = new CacheInformation();
		
		// Prepare XML parser
		RootElement root = prepareAPIKeyInfoXmlParser(result, cacheInformation);
		
		// Query API
		if (!queryApi(root.getContentHandler(), URL, keyID, vCode)) {
			return null;
		}
		
		// Plausibility check
		if (result.accessMask == 0 ||
			result.type == null ||
			result.characters.size() == 0) {
			return null;
		}

		// Cache result
		apiCache.cache(cacheKey, cacheInformation);
		
		return result;
	}
	
	private void prepareCacheInformationXmlParser(RootElement root, final CacheInformation cacheInformation) {
		root.getChild("currentTime").setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				try {
					cacheInformation.currentTime = dateFormatter.parse(body);
				} catch (ParseException e) {
					// Ignore parsing errors
				}
			}
		});
		root.getChild("cachedUntil").setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				try {
					cacheInformation.cachedUntil = dateFormatter.parse(body);
				} catch (ParseException e) {
					// Ignore parsing errors
				}
			}
		});
	}
	
	private RootElement prepareAPIKeyInfoXmlParser(final Account result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		prepareCacheInformationXmlParser(root, cacheInformation);
		
		root.getChild("result").getChild("key").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				try {
					result.accessMask = Long.parseLong(attributes.getValue("accessMask"), 10);
					result.type = attributes.getValue("type");
					String expires = attributes.getValue("expires");
					if (!"".equals(expires)) {
						result.expires = dateFormatter.parse(expires);
					}
				} catch (Exception e) {
					// Ignore any errors
				}
			}
		});
		root.getChild("result").getChild("key").getChild("rowset").getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				Character newChar = new Character();
				newChar.characterID = attributes.getValue("characterID");
				newChar.characterName = attributes.getValue("characterName");
				newChar.corporationID = attributes.getValue("corporationID");
				newChar.corporationName = attributes.getValue("corporationName");
				newChar.allianceID = attributes.getValue("allianceID");
				newChar.allianceName = attributes.getValue("allianceName");

				result.characters.add(newChar);
			}
		});
		
		return root;
	}
	
	public AccountBalance queryAccountBalance(String keyID, String vCode, String characterID) {
		final String URL = "/char/AccountBalance.xml.aspx";
		
		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(URL, keyID, vCode, characterID);
		if (apiCache.isCached(cacheKey)) {
			return null;
		}

		ArrayList<AccountBalance> result = new ArrayList<>(1);
		CacheInformation cacheInformation = new CacheInformation();
		
		// Prepare XML parser
		RootElement root = prepareAccountBalanceXmlParser(result, cacheInformation);
		
		// Query API
		if (!queryApi(root.getContentHandler(), URL, keyID, vCode, characterID)) {
			return null;
		}
		
		// Plausibility check
		if (result.isEmpty() || result.get(0).accountID == null || result.get(0).accountKey == null) {
			return null;
		}

		// Cache result
		apiCache.cache(cacheKey, cacheInformation);

		return result.get(0);
	}

	public List<AccountBalance> queryCorporationAccountBalance(String keyID, String vCode, String corporationID) {
		final String URL = "/corp/AccountBalance.xml.aspx";

		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(URL, keyID, vCode, corporationID);
		if (apiCache.isCached(cacheKey)) {
			return null;
		}

		ArrayList<AccountBalance> result = new ArrayList<>();
		CacheInformation cacheInformation = new CacheInformation();

		// Prepare XML parser
		RootElement root = prepareAccountBalanceXmlParser(result, cacheInformation);

		// Query API
		if (!queryApi(root.getContentHandler(), URL, keyID, vCode, corporationID)) {
			return null;
		}

		// Plausibility check
		if (result.isEmpty()) {
			return null;
		}

		// Cache result
		apiCache.cache(cacheKey, cacheInformation);

		return result;
	}

	private RootElement prepareAccountBalanceXmlParser(final List<AccountBalance> result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		prepareCacheInformationXmlParser(root, cacheInformation);
		
		root.getChild("result").getChild("rowset").getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				AccountBalance accountBalance = new AccountBalance();
				accountBalance.accountID = attributes.getValue("accountID");
				accountBalance.accountKey = attributes.getValue("accountKey");
				try {
					accountBalance.balance = new BigDecimal(attributes.getValue("balance"));
				} catch (NumberFormatException e) {
					// Ignore error, leave balance as 0.0
				}

				result.add(accountBalance);
			}
		});
		
		return root;
	}

	public Map<String, String> queryCorpAccountKeys(String keyID, String vCode, String corporationID) {
		final String URL = "/corp/CorporationSheet.xml.aspx";

		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(URL, keyID, vCode, corporationID);
		if (apiCache.isCached(cacheKey)) {
			return null;
		}

		HashMap<String, String> result = new HashMap<>();
		CacheInformation cacheInformation = new CacheInformation();

		// Prepare XML parser
		RootElement root = prepareCorporationSheetXmlParser(result, cacheInformation);

		// Query API
		if (!queryApi(root.getContentHandler(), URL, keyID, vCode)) {
			return null;
		}

		// Plausibility check
		if (result.isEmpty()) {
			return null;
		}

		// Cache result
		apiCache.cache(cacheKey, cacheInformation);

		return result;
	}

	private RootElement prepareCorporationSheetXmlParser(final HashMap<String, String> result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		prepareCacheInformationXmlParser(root, cacheInformation);

		final WalletDivisionsListener walletDivisionsListener = new WalletDivisionsListener();
		Element rowsetElement = root.getChild("result").getChild("rowset");
		rowsetElement.setStartElementListener(walletDivisionsListener);
		rowsetElement.getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if (walletDivisionsListener.isWalletDivision()) {
					String accountKey = attributes.getValue("accountKey");
					String description = attributes.getValue("description");
					result.put(accountKey, description);
				}
			}
		});

		return root;
	}

	private static class WalletDivisionsListener implements StartElementListener {
		private boolean walletDivision;

		public boolean isWalletDivision() {
			return walletDivision;
		}

		@Override
		public void start(Attributes attributes) {
			walletDivision = "walletDivisions".equals(attributes.getValue("name"));
		}
	}

	public List<WalletJournal> queryWallet(String keyID, String vCode, String characterID) {
		final String journalURL = "/char/WalletJournal.xml.aspx";
		final String transactionsURL = "/char/WalletTransactions.xml.aspx";
		
		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(journalURL, keyID, vCode, characterID);
		if (apiCache.isCached(cacheKey)) {
			return null;
		}
		
		CacheInformation cacheInformation = new CacheInformation();
		
		
		// Wallet transactions
		// With transaction ID as key
		HashMap<Long, WalletTransaction> walletTransactionsById = new HashMap<Long, WalletTransaction>();
		// With journal ID as key
		HashMap<Long, WalletTransaction> walletTransactionsByJournalId = new HashMap<Long, WalletTransaction>();

		// Prepare XML parser
		HashMap<Long, WalletTransaction> walletTransactionsByIdBatch = new HashMap<Long, WalletTransaction>();
		HashMap<Long, WalletTransaction> walletTransactionsByJournalIdBatch = new HashMap<Long, WalletTransaction>();
		RootElement root = prepareWalletTransactionXmlParser(walletTransactionsByIdBatch, walletTransactionsByJournalIdBatch);
		
		// Query in batches of 2560 entries
		if (!queryApi(root.getContentHandler(), transactionsURL, keyID, vCode, characterID, "2560", null)) {
			return null;
		}
		Log.d("EveApi", "Transactions loaded: " + walletTransactionsByIdBatch.size());
		while (walletTransactionsByIdBatch.size() == 2560) {
			// Find lowest transactionID
			long lowestTransactionID = Long.MAX_VALUE;
			for (Long transactionID : walletTransactionsByIdBatch.keySet()) {
				lowestTransactionID = Math.min(lowestTransactionID, transactionID);
			}
			
			// Finish batch
			walletTransactionsById.putAll(walletTransactionsByIdBatch);
			walletTransactionsByIdBatch.clear();
			walletTransactionsByJournalId.putAll(walletTransactionsByJournalIdBatch);
			walletTransactionsByJournalIdBatch.clear();
			
			// Query next batch
			if (!queryApi(root.getContentHandler(), transactionsURL, keyID, vCode, characterID, "2560", Long.toString(lowestTransactionID))) {
				return null;
			}
			Log.d("EveApi", "Transactions loaded: " + walletTransactionsByIdBatch.size());
		}
		walletTransactionsById.putAll(walletTransactionsByIdBatch);
		walletTransactionsByIdBatch.clear();
		walletTransactionsByJournalId.putAll(walletTransactionsByJournalIdBatch);
		walletTransactionsByJournalIdBatch.clear();
		
		
		// Wallet journal
		ArrayList<WalletJournal> walletJournal = new ArrayList<WalletJournal>();
		
		// Prepare XML parser
		ArrayList<WalletJournal> walletJournalBatch = new ArrayList<WalletJournal>();
		root = prepareWalletJournalXmlParser(walletJournalBatch, cacheInformation);
		
		// Query in batches of 2560 entries
		if (!queryApi(root.getContentHandler(), journalURL, keyID, vCode, characterID, "2560", null)) {
			return null;
		}
		Log.d("EveApi", "Journals loaded: " + walletJournalBatch.size());
		while (walletJournalBatch.size() == 2560) {
			// Find lowest refID
			long lowestRefID = Long.MAX_VALUE;
			for (WalletJournal journalEntry : walletJournalBatch) {
				lowestRefID = Math.min(lowestRefID, journalEntry.refID);
			}
			
			// Finish batch
			walletJournal.addAll(walletJournalBatch);
			walletJournalBatch.clear();
			
			// Query next batch
			if (!queryApi(root.getContentHandler(), journalURL, keyID, vCode, characterID, "2560", Long.toString(lowestRefID))) {
				return null;
			}
			Log.d("EveApi", "Journals loaded: " + walletJournalBatch.size());
		}
		walletJournal.addAll(walletJournalBatch);
		walletJournalBatch.clear();
		
		
		// Save all transactions; we need it later
		HashSet<WalletTransaction> allTransactions = new HashSet<WalletTransaction>(walletTransactionsById.values());
		
		// Link transactions to journal
		for (WalletJournal journalEntry : walletJournal) {
			if (journalEntry.refTypeID == 2) {
				try {
					// Market transaction
					long transactionID = Long.parseLong(journalEntry.argName1);
					WalletTransaction transaction = walletTransactionsById.get(transactionID);
					journalEntry.transaction = transaction;
					allTransactions.remove(transaction);
				} catch (Exception e) {
					// Ignore error, do not link this record
				}
			} else if (journalEntry.refTypeID == 42) {
				// Market escrow
				WalletTransaction transaction = walletTransactionsByJournalId.get(journalEntry.refID);
				journalEntry.transaction = transaction;
				allTransactions.remove(transaction);
				
				// Check if amount in journal entry equals to sum in transaction
				//if (transaction != null) {
					//BigDecimal transactionSum = transaction.price.multiply(new BigDecimal(transaction.quantity));
					//if (transactionSum.compareTo(journalEntry.amount) != 0) {
						// Some items have been payed by market escrow, some items have been payed by wallet
						// Unfortunately, number of items may be fractional.
						// That's why we don't handle this special case.
					//}
				//}
			}
		}
		
		// Create fake entries for transactions without a corresponding journal entry
		// (e.g. transactions payed by market escrow)
		for (WalletTransaction transaction : allTransactions) {
			WalletJournal fakeJournal = new WalletJournal();
			fakeJournal.date = transaction.date;
			fakeJournal.refID = 0;
			fakeJournal.refTypeID = -42;  // 42 = Market escrow; -42 = same, but already payed
			fakeJournal.ownerName1 = "";
			fakeJournal.ownerName2 = "";
			fakeJournal.argName1 = "";
			fakeJournal.amount = transaction.price.multiply(new BigDecimal(transaction.quantity));
			if ("buy".equals(transaction.transactionType)) {
				fakeJournal.amount = fakeJournal.amount.negate();
			}
			fakeJournal.taxAmount = BigDecimal.ZERO;
			fakeJournal.transaction = transaction;
			walletJournal.add(fakeJournal);
		}
		
		// Empty journal indicates some temporary error in API
		if (walletJournal.isEmpty()) {
			return null;
		}
		
		return walletJournal;
	}
	
	private RootElement prepareWalletJournalXmlParser(final ArrayList<WalletJournal> result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		prepareCacheInformationXmlParser(root, cacheInformation);
		
		root.getChild("result").getChild("rowset").getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				try {
					WalletJournal journalEntry = new WalletJournal();
					journalEntry.date = dateFormatter.parse(attributes.getValue("date"));
					journalEntry.refID = Long.parseLong(attributes.getValue("refID"));
					journalEntry.refTypeID = Integer.parseInt(attributes.getValue("refTypeID"));
					journalEntry.ownerName1 = attributes.getValue("ownerName1");
					journalEntry.ownerName2 = attributes.getValue("ownerName2");
					journalEntry.argName1 = attributes.getValue("argName1");
					journalEntry.amount = new BigDecimal(attributes.getValue("amount"));
					String taxAmount = attributes.getValue("taxAmount");
					journalEntry.taxAmount = "".equals(taxAmount) ? BigDecimal.ZERO : new BigDecimal(attributes.getValue("taxAmount"));
					
					result.add(journalEntry);
				} catch (Exception e) {
					Log.e("EveApi", "Journal parsing error", e);
					// Ignore error, do not add this record
				}
			}
		});
		
		return root;
	}
	
	private RootElement prepareWalletTransactionXmlParser(final HashMap<Long, WalletTransaction> resultById,
			final HashMap<Long, WalletTransaction> resultByJournalId) {
		RootElement root = new RootElement("eveapi");
		// Do not catch cache information, this has been done in wallet journal already
		
		root.getChild("result").getChild("rowset").getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				try {
					WalletTransaction transaction = new WalletTransaction();
					transaction.date = dateFormatter.parse(attributes.getValue("transactionDateTime"));
					Long transactionID = Long.valueOf(attributes.getValue("transactionID"));
					transaction.transactionID = transactionID;
					transaction.quantity = Integer.parseInt(attributes.getValue("quantity"));
					transaction.typeName = attributes.getValue("typeName");
					transaction.typeID = attributes.getValue("typeID");
					transaction.price = new BigDecimal(attributes.getValue("price"));
					transaction.clientName = attributes.getValue("clientName");
					transaction.stationName = attributes.getValue("stationName");
					transaction.transactionType = attributes.getValue("transactionType");
					transaction.transactionFor = attributes.getValue("transactionFor");
					Long journalTransactionID = Long.valueOf(attributes.getValue("journalTransactionID"));
					transaction.journalTransactionID = journalTransactionID;
					
					resultById.put(transactionID, transaction);
					resultByJournalId.put(journalTransactionID, transaction);
				} catch (Exception e) {
					Log.e("EveApi", "Transaction parsing error", e);
					// Ignore error, do not add this record
				}
			}
		});
		
		return root;
	}
	
	private RootElement prepareMarketOrderXmlParser(final List<MarketOrder> result, final CacheInformation cacheInformation) {
		RootElement root = new RootElement("eveapi");
		prepareCacheInformationXmlParser(root, cacheInformation);
		
		root.getChild("result").getChild("rowset").getChild("row").setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				try {
					MarketOrder marketOrder = new MarketOrder();
					marketOrder.orderID = Long.parseLong(attributes.getValue("orderID"));
					marketOrder.stationID = Integer.parseInt(attributes.getValue("stationID"));
					marketOrder.volEntered = Long.parseLong(attributes.getValue("volEntered"));
					marketOrder.volRemaining = Long.parseLong(attributes.getValue("volRemaining"));
					marketOrder.orderState = Integer.parseInt(attributes.getValue("orderState"));
					marketOrder.typeID = Integer.parseInt(attributes.getValue("typeID"));
					marketOrder.duration = Integer.parseInt(attributes.getValue("duration"));
					marketOrder.price = new BigDecimal(attributes.getValue("price"));
					marketOrder.bid = Integer.parseInt(attributes.getValue("bid"));
					marketOrder.issued = dateFormatter.parse(attributes.getValue("issued"));
					
					result.add(marketOrder);
				} catch (Exception e) {
					Log.e("EveApi", "Market order parsing error", e);
					// Ignore error, do not add this record
				}
			}
		});
		
		return root;
	}
	
	public List<MarketOrder> queryMarketOrders(String keyID, String vCode, String characterID) {
		final String marketOrderURL = "/char/MarketOrders.xml.aspx";
		
		// Lookup in cache
		String cacheKey = CacheInformation.buildHashKey(marketOrderURL, keyID, vCode, characterID);
		if (apiCache.isCached(cacheKey)) {
			return null;
		}
		
		CacheInformation cacheInformation = new CacheInformation();

		// Prepare XML parser
		ArrayList<MarketOrder> result = new ArrayList<MarketOrder>();
		RootElement root = prepareMarketOrderXmlParser(result, cacheInformation);
		
		// Query API
		if (!queryApi(root.getContentHandler(), marketOrderURL, keyID, vCode, characterID)) {
			return null;
		}
		Log.d("EveApi", "Market orders loaded: " + result.size());
		
		// Plausibility check
		if (result.isEmpty()) {
			return null;
		}
		
		// Cache result
		apiCache.cache(cacheKey, cacheInformation);
		
		return result;
	}

}
