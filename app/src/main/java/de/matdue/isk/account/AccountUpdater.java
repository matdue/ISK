/**
 * Copyright 2015 Matthias Düsterhöft
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
package de.matdue.isk.account;

import android.accounts.Account;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.matdue.isk.IskApplication;
import de.matdue.isk.MarketOrderActivity;
import de.matdue.isk.NotificationDeletedReceiver;
import de.matdue.isk.R;
import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.ApiAccount;
import de.matdue.isk.database.Balance;
import de.matdue.isk.database.EveDatabase;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.database.OrderWatch;
import de.matdue.isk.database.Wallet;
import de.matdue.isk.eve.*;

/**
 * Updates a specific account: Balance, wallet and market orders
 */
public class AccountUpdater {

    private Context context;
    private Account account;
    private ApiKey apiKey;
    private IskDatabase iskDatabase;
    private EveDatabase eveDatabase;
    private EveApi eveApi;
    private BitmapManager bitmapManager;

    public AccountUpdater(Account account, String token, IskApplication iskApplication, boolean forcedUpdate) {
        context = iskApplication;
        this.account = account;

        apiKey = new ApiKey(token);
        iskDatabase = iskApplication.getIskDatabase();
        eveDatabase = iskApplication.getEveDatabase();
        eveApi = new EveApi(new EveApiCacheDatabase(forcedUpdate));
        bitmapManager = iskApplication.getBitmapManager();
    }

    public int updateCharacter() throws UnknownAccountException, InvalidAccountException, EveApiException {
        ApiAccount apiAccount = iskDatabase.queryApiAccount(account.name);
        if (apiAccount == null) {
            throw new UnknownAccountException(account.name);
        }

        int updates;
        updates = updateApiAccount(apiAccount);
        updates += updateBalance(apiAccount.characterId);
        updates += updateWallet(apiAccount.characterId);
        updates += updateMarketOrders(apiAccount.characterId);

        // Notify about updates
        submitNotification(apiAccount.characterId);

        return updates;
    }

    /**
     * Update the character's corporation and alliance data.
     *
     * @param apiAccount the account
     * @return Number of updates.
     */
    private int updateApiAccount(ApiAccount apiAccount) throws InvalidAccountException, EveApiException {
        de.matdue.isk.eve.Account account = eveApi.validateKey(apiKey.getKeyID(), apiKey.getVCode());
        if (account != null && account.errorCode != null) {
            throw new InvalidAccountException(account.errorText);
        }
        if (account != null && !account.isCorporation() && account.characters != null) {
            for (de.matdue.isk.eve.Character character : account.characters) {
                if (apiAccount.characterId.equals(character.characterID)) {
                    apiAccount.corporationId = character.corporationID;
                    apiAccount.corporationName = character.corporationName;
                    apiAccount.allianceId = character.allianceID;
                    apiAccount.allianceName = character.allianceName;
                    iskDatabase.storeApiAccount(apiAccount);

                    return 1;
                }
            }
        }

        return 0;
    }

    /**
     * Update a character's balance.
     *
     * @param characterId Character's EVE id
     * @return Number of updates balances.
     */
    private int updateBalance(String characterId) throws EveApiException {
        AccountBalance accountBalance = eveApi.queryAccountBalance(apiKey.getKeyID(), apiKey.getVCode(), characterId);
        if (accountBalance != null) {
            Balance balance = new Balance();
            balance.balance = accountBalance.balance;
            balance.characterId = characterId;
            iskDatabase.storeBalance(balance);
            return 1;
        }

        return 0;
    }

    private int updateWallet(String characterId) throws EveApiException {
        List<WalletJournal> wallet = eveApi.queryWallet(apiKey.getKeyID(), apiKey.getVCode(), characterId);
        if (wallet != null) {
            // Prepare data objects
            ArrayList<Wallet> walletDatas = new ArrayList<>();
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
            return walletDatas.size();
        }

        return 0;
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
            if (typeName == null) {
                typeName = context.getString(R.string.market_order_unknown_item, Integer.toString(marketOrder.typeID));
            }
            itemNameCache.put(marketOrder.typeID, typeName);
        }
        orderWatch.typeName = typeName;

        orderWatch.stationID = marketOrder.stationID;
        String stationName = stationNameCache.get(marketOrder.stationID);
        if (stationName == null) {
            stationName = eveDatabase.queryStationName(marketOrder.stationID);
            if (stationName == null) {
                stationName = context.getString(R.string.market_order_unknown_station, Integer.toString(marketOrder.stationID));
            }
            stationNameCache.put(marketOrder.stationID, stationName);
        }
        orderWatch.stationName = stationName;

        orderWatch.price = marketOrder.price;
        orderWatch.volEntered = marketOrder.volEntered;
        orderWatch.volRemaining = marketOrder.volRemaining;
        orderWatch.fulfilled = (int) (100 - ((marketOrder.volEntered != 0) ? (100 * marketOrder.volRemaining / marketOrder.volEntered) : 0));
        if (orderWatch.fulfilled == 100 && marketOrder.volRemaining != marketOrder.volEntered) {
            // Make sure nearly completed orders do not reach 100%
            // because of rounding
            --orderWatch.fulfilled;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(marketOrder.issued);
        calendar.add(Calendar.DATE, marketOrder.duration);
        orderWatch.expires = calendar.getTime();

        orderWatch.action = marketOrder.bid;
        orderWatch.status = 0;
        orderWatch.sortKey = 0;

        return orderWatch;
    }

    private int updateMarketOrders(String characterId) throws EveApiException {
        List<MarketOrder> marketOrders = eveApi.queryMarketOrders(apiKey.getKeyID(), apiKey.getVCode(), characterId);
        if (marketOrders != null) {
            long seqId = SystemClock.elapsedRealtime();

            // Load old data objects (for expired orders)
            List<OrderWatch> oldOrderWatches = iskDatabase.queryAllOrderWatches(characterId);

            // Prepare data objects
            ArrayList<OrderWatch> orderWatches = new ArrayList<>();

            // Copy expired orders
            for (OrderWatch oldOrderWatch : oldOrderWatches) {
                if (oldOrderWatch.orderID == 0) {
                    oldOrderWatch.seqId = ++seqId;
                    orderWatches.add(oldOrderWatch);
                }
            }

            // Cache for item name and station name to minimize database access
            SparseArray<String> itemNameCache = new SparseArray<>();
            SparseArray<String> stationNameCache = new SparseArray<>();

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
                        // Watch this order if the same type ID has been watched in the past
                        boolean shouldWatch = iskDatabase.shouldWatchItem(characterId, orderWatch.typeID, orderWatch.action);
                        if (shouldWatch) {
                            orderWatch.status |= OrderWatch.WATCH;
                        }
                    }

                    orderWatches.add(orderWatch);
                } else {
                    // Inactive order: If it was watched, remember it
                    if (savedOrderWatch != null && (savedOrderWatch.status & OrderWatch.WATCH) != 0) {
                        orderWatch.orderID = 0;
                        orderWatch.sortKey = 0;
                        orderWatch.status &= ~(OrderWatch.NOTIFIED_AND_READ | OrderWatch.NOTIFIED);
                        orderWatches.add(orderWatch);
                    }
                }
            }

            // Mark missing orders as expired/fulfilled
            // but only those who are watched
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
                if (isMissing && (oldOrderWatch.status & OrderWatch.WATCH) != 0) {
                    // Add as inactive order
                    oldOrderWatch.orderID = 0;
                    oldOrderWatch.sortKey = 0;
                    oldOrderWatch.status &= ~(OrderWatch.NOTIFIED_AND_READ | OrderWatch.NOTIFIED);
                    orderWatches.add(oldOrderWatch);
                }
            }

            iskDatabase.storeOrderWatches(characterId, orderWatches);
            return orderWatches.size();
        }

        return 0;
    }

    private void submitNotification(String characterId) {
        // Get notification settings (ringtone etc.)
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean doNotify = preferences.getBoolean("notification", true);
        if (!doNotify) {
            return;
        }
        String ringtoneUri = preferences.getString("ringtone", "");
        String vibration = preferences.getString("vibration", "");
        boolean doVibrate = "1".equals(vibration);
        if ("2".equals(vibration)) {
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                doVibrate = true;
            }
        }
        boolean showLights = preferences.getBoolean("lights", true);

        // Any market order available which has not been notified?
        boolean hasUnnotifiedOrders = iskDatabase.hasUnreadOrderWatches(characterId);
        if (!hasUnnotifiedOrders) {
            return;
        }
        iskDatabase.setOrderWatchStatusBits(characterId, OrderWatch.NOTIFIED);

        // Fetch unnotified market orders
        Cursor marketOrderCursor = iskDatabase.getJustEndedOrderWatches(characterId);
        if (marketOrderCursor == null) {
            return;
        }

        ArrayList<String> itemNames = new ArrayList<>();
        while (marketOrderCursor.moveToNext()) {
            itemNames.add(marketOrderCursor.getString(1));
        }
        marketOrderCursor.close();
        if (itemNames.isEmpty()) {
            return;
        }

        // Intent when user clicks notification
        Intent notificationIntent = new Intent(context, MarketOrderActivity.class);
        notificationIntent.putExtra("characterID", characterId);
        notificationIntent.putExtra("characterName", account.name);
        PendingIntent contentIntent = PendingIntent.getActivity(context, account.name.hashCode(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent when user clears notification
        Intent notificationDeletedIntent = new Intent(context, NotificationDeletedReceiver.class);
        notificationDeletedIntent.putExtra("characterID", characterId);
        notificationDeletedIntent.putExtra("characterName", account.name);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, account.name.hashCode(), notificationDeletedIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Resources resources = context.getResources();
        int defaults = 0;
        if (doVibrate) {
            defaults |= Notification.DEFAULT_VIBRATE;
        }
        if (showLights) {
            defaults |= Notification.DEFAULT_LIGHTS;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_isk)
                .setContentTitle(resources.getText(R.string.market_order_notification_title))
                .setContentText(TextUtils.join(", ", itemNames))
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setTicker(resources.getText(R.string.market_order_notification_ticker))
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true)
                .setDefaults(defaults);
        if (!"".equals(ringtoneUri)) {
            builder.setSound(Uri.parse(ringtoneUri));
        }
        if (itemNames.size() > 1) {
            builder.setNumber(itemNames.size());
        }

        // Inbox style
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        int itemsToAdd = Math.min(5, itemNames.size());
        for (int i = 0; i < itemsToAdd; ++i) {
            inboxStyle.addLine(itemNames.get(i));
        }
        if (itemNames.size() > 5) {
            int additional = itemNames.size() - 5;
            inboxStyle.setSummaryText(context.getResources().getString(R.string.market_order_notification_summary, additional));
        }
        builder.setStyle(inboxStyle);

        // Use portrait as large icon
        Bitmap portrait = bitmapManager.getImage(EveApi.getCharacterUrl(characterId, 128));
        if (portrait != null) {
            Bitmap largeIcon = Bitmap.createScaledBitmap(
                    portrait,
                    resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                    resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                    false);
            builder.setLargeIcon(largeIcon);
        }

        // Create notification and submit it
        Notification notification = builder.build();
        NotificationManagerCompat.from(context)
                .notify(account.name, R.id.market_order_notification, notification);
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
