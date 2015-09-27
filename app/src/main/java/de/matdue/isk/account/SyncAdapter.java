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
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import de.matdue.isk.EveApiUpdater;

/**
 * Sync adapter performing updating our accounts
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private Context context;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.context = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        AccountManager accountManager = AccountManager.get(context);
        try {
            Boolean isCharacter = accountManager.hasFeatures(account, new String[]{"apiCharacter"}, null, null).getResult();
            if (isCharacter != null && isCharacter) {
                String token = accountManager.blockingGetAuthToken(account, AccountAuthenticator.AUTHTOKEN_TYPE_API, true);
                Log.d("SyncAdapter", "Character " + account.name + "; " + (token != null ? token : "<null>"));
                if (token != null) {
                    ApiKey apiKey = new ApiKey(token);
                    Log.d("SyncAdapter", apiKey.toString());

                    EveApiUpdater eveApiUpdater = new EveApiUpdater(context, true);
                    eveApiUpdater.updateBalance(apiKey);
                }
            }

            Boolean isCorporation = accountManager.hasFeatures(account, new String[]{"apiCorporation"}, null, null).getResult();
            if (isCorporation != null && isCorporation) {
                String token = accountManager.blockingGetAuthToken(account, AccountAuthenticator.AUTHTOKEN_TYPE_API, true);
                Log.d("SyncAdapter", "Corporation " + account.name + "; " + (token != null ? token : "<null>"));
                if (token != null) {
                    ApiKey apiKey = new ApiKey(token);
                    Log.d("SyncAdapter", apiKey.toString());

                    EveApiUpdater eveApiUpdater = new EveApiUpdater(context, true);
                    eveApiUpdater.updateCorporationBalance(apiKey);
                }
            }

            syncResult.stats.numAuthExceptions = 0;  // Hard error
            syncResult.stats.numIoExceptions = 0;  // Soft error
            syncResult.stats.numUpdates = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
