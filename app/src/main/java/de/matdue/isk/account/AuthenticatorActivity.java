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
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.matdue.isk.MainActivity;
import de.matdue.isk.R;

/**
 * The authentication activity.
 *
 * Called by authenticator to do all UI stuff.
 *
 * For API key, the Android account has the following user data:
 * <ul>
 *     <li>id: API key id</li>
 *     <li>api: {@link AccountAuthenticator#AUTHTOKEN_TYPE_API_CORPORATION} or {@link AccountAuthenticator#AUTHTOKEN_TYPE_API_CHARACTER}</li>
 *     <li>characterID: EVE Online charactor or corporation ID</li>
 * </ul>
 * Account's property {@link Account#name} is the name of the charactor or corporation.
 * Property {@link Account#type} is always <code>de.matdue.isk</code>.
 * An account has either a charactor or corporation token. The token type is either
 * {@link AccountAuthenticator#AUTHTOKEN_TYPE_API_CORPORATION} or {@link AccountAuthenticator#AUTHTOKEN_TYPE_API_CHARACTER}.
 * For CREST account, another token will be added of a different type.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    /**
     * Argument key for account type id; always de.matdue.isk
     */
    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";

    /**
     * Argument key for token type; one of AccountAuthenticator.AUTHTOKEN_TYPE...
     */
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";

    /**
     * Argument key for account name; this is the charactor or coporation name
     */
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";

    /**
     * Argument key for indicator whether this activity is called for a new or an existing account
     */
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    private final static int CHECK_API_KEY_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
        if (accountType == null) {
            // Not called from AccountAuthenticator => behave as normal activity
            // and allow link to home page on action bar
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.authenticator);

        boolean isNewAccount = getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false);
        if (!isNewAccount) {
            // Authentication failed; display appropriate intro text
            String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
            String introText = getString(R.string.account_authentication_failed_intro, accountName);
            TextView introTextView = (TextView) findViewById(R.id.key_authentication_failed_intro);
            introTextView.setText(introText);

            findViewById(R.id.key_authentication_intro).setVisibility(View.GONE);
            introTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Handle back button on action bar
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * User wants to use an existing API key.
     *
     * @param view Button clicked
     */
    public void gotoAskKey(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://community.eveonline.com/support/api-key/ActivateInstallLinks?activate=true"));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(browserIntent);
    }

    /**
     * User wants to create a new API key.
     *
     * @param view Button clicked
     */
    public void gotoCreateKey(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://community.eveonline.com/support/api-key/CreatePredefined?accessMask=" + CheckApiKeyActivity.ACCESS_MASK));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(browserIntent);
    }

    /**
     * User entered ID and verification code of an API key.
     *
     * @param view Button clicked
     */
    public void gotoSubmitKey(View view) {
        String keyID = ((TextView)findViewById(R.id.key_input_id)).getText().toString();
        String vCode = ((TextView)findViewById(R.id.key_input_vCode)).getText().toString();
        validateApiKey(keyID, vCode);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // User selected an API key on EVE Online web site.
        // It is called by ApiCallbackActivity.
        // AuthenticatorActivity must be a singleton, otherwise onNewIntent() won't be called.
        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith("eve://api.eveonline.com/installKey")) {
            String id = uri.getQueryParameter("keyID");
            String code = uri.getQueryParameter("vCode");
            validateApiKey(id, code);
        }
    }

    /**
     * Validate an API key by handing over to {@link CheckApiKeyActivity}.
     *
     * @param id API key ID
     * @param code Verification code
     */
    private void validateApiKey(String id, String code) {
        if ("".equals(id) || "".equals(code)) {
            Toast.makeText(this, R.string.pilots_key_empty_fields, Toast.LENGTH_LONG).show();
            return;
        }

        Intent checkApiKeyIntent = new Intent(this, CheckApiKeyActivity.class);
        checkApiKeyIntent.putExtra("keyID", id);
        checkApiKeyIntent.putExtra("vCode", code);
        checkApiKeyIntent.putExtra("name", getIntent().getStringExtra(ARG_ACCOUNT_NAME));
        startActivityForResult(checkApiKeyIntent, CHECK_API_KEY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHECK_API_KEY_REQUEST_CODE && resultCode == RESULT_OK) {
            // CheckApiKeyActivity has finished. Create API key for all selected characters
            // or corporations and return to authenticator.
            de.matdue.isk.eve.Account account = (de.matdue.isk.eve.Account) data.getSerializableExtra("account");
            String keyID = data.getStringExtra("keyID");
            String vCode = data.getStringExtra("vCode");
            Bundle accountData = createApiKeys(account, keyID, vCode);
            if (accountData != null) {
                setAccountAuthenticatorResult(accountData);
                Intent intent = new Intent();
                intent.putExtras(accountData);
                setResult(RESULT_OK, intent);
            }
            finish();
        }
    }

    /**
     * Creates Android accounts for each charactor or corporation.
     *
     * @param account EVE Online account with character(s) or corporation(s)
     * @param keyID API key ID
     * @param vCode API key verification code
     * @return Data bundle as AccountAuthenticatorActivity expects, for the first created or updated Android account
     */
    private Bundle createApiKeys(de.matdue.isk.eve.Account account, String keyID, String vCode) {
        AccountManager accountManager = AccountManager.get(this);
        Bundle firstAccountData = null;
        ArrayList<String> createdAccounts = new ArrayList<>();
        ArrayList<String> updatedAccounts = new ArrayList<>();
        ArrayList<String> failedAccounts = new ArrayList<>();

        for (de.matdue.isk.eve.Character character : account.characters) {
            Account newAccount = new Account(account.isCorporation() ? character.corporationName : character.characterName, AccountAuthenticator.ACCOUNT_TYPE);
            Bundle userData = new Bundle();
            userData.putString("id", keyID);
            userData.putString("api", account.isCorporation() ? AccountAuthenticator.AUTHTOKEN_TYPE_API_CORPORATION : AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER);
            userData.putString("characterID", account.isCorporation() ? character.corporationID : character.characterID);

            if (accountManager.addAccountExplicitly(newAccount, null, userData)) {
                // Account created, it didn't exist before
                accountManager.setAuthToken(newAccount, account.isCorporation() ? AccountAuthenticator.AUTHTOKEN_TYPE_API_CORPORATION : AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER, vCode);
                createdAccounts.add(newAccount.name);

                if (firstAccountData == null) {
                    firstAccountData = new Bundle();
                    firstAccountData.putString(AccountManager.KEY_ACCOUNT_NAME, newAccount.name);
                    firstAccountData.putString(AccountManager.KEY_ACCOUNT_TYPE, newAccount.type);
                    firstAccountData.putString(AccountManager.KEY_AUTHTOKEN, vCode);
                }
            } else {
                // Account creation failed, does it exist already?
                boolean accountExists = false;
                Account[] accountsByType = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
                for (Account existingAccount : accountsByType) {
                    if (newAccount.name.equals(existingAccount.name)) {
                        accountExists = true;

                        // Update account
                        accountManager.setUserData(existingAccount, "id", userData.getString("id"));
                        accountManager.setUserData(existingAccount, "api", userData.getString("api"));
                        accountManager.setUserData(existingAccount, "characterID", userData.getString("characterID"));
                        accountManager.setAuthToken(existingAccount, account.isCorporation() ? AccountAuthenticator.AUTHTOKEN_TYPE_API_CORPORATION : AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER, vCode);

                        if (firstAccountData == null) {
                            firstAccountData = new Bundle();
                            firstAccountData.putString(AccountManager.KEY_ACCOUNT_NAME, existingAccount.name);
                            firstAccountData.putString(AccountManager.KEY_ACCOUNT_TYPE, existingAccount.type);
                            firstAccountData.putString(AccountManager.KEY_AUTHTOKEN, vCode);
                        }

                        break;
                    }
                }

                if (accountExists) {
                    updatedAccounts.add(newAccount.name);
                } else {
                    failedAccounts.add(newAccount.name);
                }
            }
        }

        // Prepare notice with results
        ArrayList<String> toastMessages = new ArrayList<>(3);
        if (!createdAccounts.isEmpty()) {
            toastMessages.add(String.format("Created: %1$s", TextUtils.join(", ", createdAccounts)));
        }
        if (!updatedAccounts.isEmpty()) {
            toastMessages.add(String.format("Updated: %1$s", TextUtils.join(", ", updatedAccounts)));
        }
        if (!failedAccounts.isEmpty()) {
            toastMessages.add(String.format("Failed to create: %1$s", TextUtils.join(", ", failedAccounts)));
        }
        // Display notice, if there is anything to show
        if (!toastMessages.isEmpty()) {
            Toast.makeText(this, TextUtils.join("\n", toastMessages), Toast.LENGTH_LONG).show();
        }

        return firstAccountData;
    }

}
