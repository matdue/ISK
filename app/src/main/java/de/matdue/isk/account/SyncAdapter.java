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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import de.matdue.isk.IskApplication;
import de.matdue.isk.R;
import de.matdue.isk.eve.EveApiException;

/**
 * Sync adapter performing updating our accounts
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String CONTENT_AUTHORITY = "de.matdue.isk.content.provider";
    public static final String SYNC_FINISHED_BROADCAST = "de.matdue.isk.SYNC_FINISHED";

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        String errorMessage = null;
        boolean forcedUpdate = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        try {
            // Skip update if no network is available
            if (!isNetworkAvailable()) {
                return;
            }

            // Skip update if WIFI inactive, but required
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
            boolean requireWifi = sharedPreferences.getBoolean("requireWifi", false);
            if (!forcedUpdate && requireWifi && !isWifiConnected()) {
                return;
            }

            AccountManager accountManager = AccountManager.get(getContext());
            String token = accountManager.blockingGetAuthToken(account, AccountAuthenticator.AUTHTOKEN_TYPE_API_CHAR, true);
            if (token == null) {
                // Authentication failed
                syncResult.stats.numAuthExceptions++;
                return;
            }

            Log.d("SyncAdapter", "Syncing account " + account.name);

            IskApplication iskApplication = (IskApplication) getContext().getApplicationContext();
            AccountUpdater accountUpdater = new AccountUpdater(account, token, iskApplication, forcedUpdate);
            try {
                int updates = accountUpdater.updateCharacter();
                syncResult.stats.numUpdates += updates;
            } catch (UnknownAccountException e) {
                // No corresponding database entry found for account
                // Invalidate token to force an authentication by user
                accountManager.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE, token);
                syncResult.stats.numAuthExceptions++;
                errorMessage = "Authentication failure";  // TODO: l10n
                return;
            }

            Log.d("SyncAdapter", "Finished syncing account " + account.name);
        } catch (OperationCanceledException | AuthenticatorException e) {
            // User cancelled authorization; not possible here as we do not wait for user interaction
            syncResult.stats.numAuthExceptions++;
            errorMessage = e.getMessage();
            Log.e("SyncAdapter", "Authentication failure", e);
        } catch (EveApiException e) {
            // Network error, Android will try again later
            syncResult.stats.numIoExceptions++;
            errorMessage = getContext().getString(e.isNetworkError() ? R.string.error_network : R.string.error_eve_api);
            Log.e("SyncAdapter", "Eve Api error", e);
        } catch (Exception e) {
            // Network error, Android will try again later
            syncResult.stats.numIoExceptions++;
            errorMessage = e.getMessage();
            Log.e("SyncAdapter", "I/O or any other error", e);
        } finally {
            getContext().sendBroadcast(new Intent(SYNC_FINISHED_BROADCAST)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .putExtra("account", account.name)
                    .putExtra("error", errorMessage));
        }
    }

    /**
     * Network detection
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    /**
     * WIFI detection
     */
    private boolean isWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnectedOrConnecting();
    }

}
