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

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.matdue.isk.account.ApiKey;
import de.matdue.isk.database.*;
import de.matdue.isk.eve.AccountBalance;
import de.matdue.isk.eve.CacheInformation;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCache;

/**
 * Created by Matthias on 26.08.2015.
 */
public class EveApiUpdater {

    private Context context;
    private IskDatabase iskDatabase;
    private EveDatabase eveDatabase;
    private EveApi eveApi;

    public EveApiUpdater(Context context, boolean forcedUpdate) {
        this.context = context;
        IskApplication iskApplication = (IskApplication) context.getApplicationContext();
        iskDatabase = iskApplication.getIskDatabase();
        eveDatabase = iskApplication.getEveDatabase();
        eveApi = new EveApi(new EveApiCacheDatabase(forcedUpdate));
    }

    public void updateBalance(ApiKey apiKey) {
        AccountBalance accountBalance = eveApi.queryAccountBalance(apiKey.getKeyID(), apiKey.getvCode(), apiKey.getCharacterID());
        if (accountBalance != null) {
            Balance balance = new Balance();
            balance.balance = accountBalance.balance;
            balance.characterId = apiKey.getCharacterID();
            iskDatabase.storeBalance(balance);
        }
    }

    public void updateCorporationBalance(ApiKey apiKey) {
        // Get account keys with account descriptions
        Map<String, String> accountKeys = eveApi.queryCorpAccountKeys(apiKey.getKeyID(), apiKey.getvCode(), apiKey.getCharacterID());

        // Get balances
        List<AccountBalance> accountBalances = eveApi.queryCorporationAccountBalance(apiKey.getKeyID(), apiKey.getvCode(), apiKey.getCharacterID());

        if (accountBalances != null && accountKeys != null && !accountBalances.isEmpty()) {
            ArrayList<CorporationBalance> balances = new ArrayList<>(accountBalances.size());
            for (AccountBalance accountBalance : accountBalances) {
                CorporationBalance balance = new CorporationBalance();
                balance.balance = accountBalance.balance;
                balance.corporationId = apiKey.getCharacterID();
                balance.accountKey = accountBalance.accountKey;
                balance.accountDescription = accountKeys.get(accountBalance.accountKey);
                balances.add(balance);
            }
            iskDatabase.storeCorporationBalance(balances);
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
