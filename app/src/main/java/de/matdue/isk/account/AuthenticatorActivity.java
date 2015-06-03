package de.matdue.isk.account;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.matdue.isk.IskApplication;
import de.matdue.isk.MainActivity;
import de.matdue.isk.R;
import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCacheDummy;
import de.matdue.isk.widget.ResourceListAdapter;

/**
 * Created by Matthias on 09.02.2015.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final int ACCESS_MASK = 6361217;

    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.authenticator);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        accountManager = AccountManager.get(getBaseContext());

        boolean isNewAccount = getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false);
        ((TextView)findViewById(R.id.key_text_is_new)).setText("Account ist " + (isNewAccount ? "neu" : "nicht neu"));

        String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
        ((TextView)findViewById(R.id.key_text_account_type)).setText(accountType);

        String authType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        ((TextView)findViewById(R.id.key_text_auth_type)).setText(authType);

        findViewById(R.id.key_submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        findViewById(R.id.key_ask_api_key).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://community.eveonline.com/support/api-key/ActivateInstallLinks?activate=true"));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(browserIntent);
            }
        });

        findViewById(R.id.key_goto_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent homeIntent = new Intent(AuthenticatorActivity.this, MainActivity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith("eve://api.eveonline.com/installKey")) {
            String id = uri.getQueryParameter("keyID");
            String code = uri.getQueryParameter("vCode");

            validateApiKey(id, code);
        }
    }

    private void validateApiKey(String id, String code) {
        if ("".equals(id) || "".equals(code)) {
            Toast.makeText(this, R.string.pilots_key_empty_fields, Toast.LENGTH_LONG).show();
            return;
        }

        new ApiKeyCheckingTask(this).execute(id, code);
    }

    public void submit() {
        String name = ((TextView)findViewById(R.id.key_input_name)).getText().toString();
        String keyID = ((TextView)findViewById(R.id.key_input_id)).getText().toString();
        String vCode = ((TextView)findViewById(R.id.key_input_vCode)).getText().toString();

        Bundle data = new Bundle();
        data.putString(AccountManager.KEY_ACCOUNT_NAME, name);
        data.putString(AccountManager.KEY_ACCOUNT_TYPE, getIntent().getStringExtra(ARG_ACCOUNT_TYPE));
        data.putString(AccountManager.KEY_AUTHTOKEN, vCode);

        Account account = new Account(name, data.getString(AccountManager.KEY_ACCOUNT_TYPE));
        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Bundle userData = new Bundle();
            userData.putString("keyID", keyID);
            accountManager.addAccountExplicitly(account, null, userData);

            String authTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
            if (authTokenType == null) {
                authTokenType = AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER;
            }
            accountManager.setAuthToken(account, authTokenType, vCode);
        }

        setAccountAuthenticatorResult(data);
        Intent intent = new Intent();
        intent.putExtras(data);
        setResult(RESULT_OK, intent);
        finish();
    }

    private static class ApiKeyCheckingTask extends AsyncTask<String, Void, de.matdue.isk.eve.Account> {

        private AuthenticatorActivity parent;
        private ProgressDialog waitDialog;
        private String id;
        private String vCode;

        private static class SelectedCharacter {
            public de.matdue.isk.eve.Character eveCharacter;
            public boolean corporation;
            public boolean selected;
        }

        private static class SelectCharacterAdapter extends ResourceListAdapter<SelectedCharacter> {
            private BitmapManager bitmapManager;

            public SelectCharacterAdapter(Context context, BitmapManager bitmapManager, List<SelectedCharacter> selectedCharacters) {
                super(context, R.layout.account_api_select_dialog_item, selectedCharacters);

                this.bitmapManager = bitmapManager;
            }

            @Override
            public void bindView(View view, Context context, SelectedCharacter item, boolean checked) {
                // ViewHolder pattern
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                if (viewHolder == null) {
                    viewHolder = new ViewHolder();
                    viewHolder.image = (ImageView) view.findViewById(R.id.account_api_select_dialog_item_image);
                    viewHolder.text = (CheckedTextView) view.findViewById(R.id.account_api_select_dialog_item_text);

                    view.setTag(viewHolder);
                }

                bitmapManager.setImageBitmap(viewHolder.image,
                        item.corporation ? EveApi.getCorporationUrl(item.eveCharacter.corporationID, 128) : EveApi.getCharacterUrl(item.eveCharacter.characterID, 128),
                        null, null);

                viewHolder.text.setText(item.corporation ? item.eveCharacter.corporationName : item.eveCharacter.characterName);
                viewHolder.text.setChecked(checked);
            }

            static class ViewHolder {
                ImageView image;
                CheckedTextView text;
            }
        }

        public ApiKeyCheckingTask(AuthenticatorActivity parent) {
            this.parent = parent;
        }

        @Override
        protected void onPreExecute() {
            waitDialog = ProgressDialog.show(parent, "", parent.getString(R.string.pilots_key_checking), true, true, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel(false);
                    dialogInterface.dismiss();
                }
            });
        }

        @Override
        protected de.matdue.isk.eve.Account doInBackground(String... params) {
            id = params[0];
            vCode = params[1];

            EveApi api = new EveApi(new EveApiCacheDummy());
            de.matdue.isk.eve.Account apiAccount = api.validateKey(id, vCode);
            return apiAccount;
        }

        @Override
        protected void onPostExecute(de.matdue.isk.eve.Account apiAccount) {
            waitDialog.dismiss();

            if (isCancelled()) {
                return;
            }

            if (apiAccount == null) {
                Toast.makeText(parent, R.string.pilots_key_error_validate, Toast.LENGTH_LONG).show();
                return;
            }

            if ((apiAccount.accessMask & ACCESS_MASK) != ACCESS_MASK) {
                Toast.makeText(parent, R.string.pilots_key_error_accessmask, Toast.LENGTH_LONG).show();
                return;
            }

            selectAccounts(apiAccount);
        }

        private void selectAccounts(de.matdue.isk.eve.Account apiAccount) {
            final ArrayList<SelectedCharacter> selectedCharacters = new ArrayList<SelectedCharacter>(apiAccount.characters.size());
            for (de.matdue.isk.eve.Character eveCharacter : apiAccount.characters) {
                SelectedCharacter selectedCharacter = new SelectedCharacter();
                selectedCharacter.eveCharacter = eveCharacter;
                selectedCharacter.corporation = "Corporation".equals(apiAccount.type);
                selectedCharacter.selected = true;
                selectedCharacters.add(selectedCharacter);
            }

            if (selectedCharacters.size() <= 1) {
                createAccountsAndFinish(selectedCharacters);
                return;
            }

            SelectCharacterAdapter adapter = new SelectCharacterAdapter(parent, ((IskApplication) parent.getApplication()).getBitmapManager(), selectedCharacters);

            View dialogView = parent.getLayoutInflater().inflate(R.layout.account_api_select_dialog, null);
            final ListView dialogListView = (ListView) dialogView.findViewById(R.id.account_api_select_dialog_pilots);
            dialogListView.setAdapter(adapter);
            for (int i = 0; i < selectedCharacters.size(); ++i) {
                dialogListView.setItemChecked(i, true);
            }

            AlertDialog dialog = new AlertDialog.Builder(parent)
                    .setTitle("Piloten")
                    .setPositiveButton("Konto/Konten erstellen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for (i = 0; i < selectedCharacters.size(); ++i) {
                                selectedCharacters.get(i).selected = dialogListView.isItemChecked(i);
                            }

                            createAccountsAndFinish(selectedCharacters);
                        }
                    })
                    .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            parent.finish();
                        }
                    })
                    .setView(dialogView)
                    .create();

            dialog.show();
        }

        private void createAccountsAndFinish(List<SelectedCharacter> selectedCharacters) {
            Bundle accountData = createAccounts(selectedCharacters);
            if (accountData != null) {
                parent.setAccountAuthenticatorResult(accountData);
                Intent intent = new Intent();
                intent.putExtras(accountData);
                parent.setResult(RESULT_OK, intent);
            }
            parent.finish();
        }

        private Bundle createAccounts(List<SelectedCharacter> selectedCharacters) {
            AccountManager accountManager = AccountManager.get(parent);
            Bundle firstAccountData = null;
            ArrayList<String> createdAccounts = new ArrayList<String>();
            ArrayList<String> failedAccounts = new ArrayList<String>();

            for (SelectedCharacter character : selectedCharacters) {
                if (!character.selected) {
                    continue;
                }

                Account newAccount = new Account(character.corporation ? character.eveCharacter.corporationName : character.eveCharacter.characterName, AccountAuthenticator.ACCOUNT_TYPE);
                Bundle userData = new Bundle();
                userData.putString("id", id);
                userData.putString("vCode", vCode);
                userData.putString("api", character.corporation ? AccountAuthenticator.AUTHTOKEN_TYPE_API_CORPORATION : AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER);
                userData.putString("characterID", character.corporation ? character.eveCharacter.corporationID : character.eveCharacter.characterID);

                if (accountManager.addAccountExplicitly(newAccount, null, userData)) {
                    accountManager.setAuthToken(newAccount, character.corporation ? AccountAuthenticator.AUTHTOKEN_TYPE_API_CORPORATION : AccountAuthenticator.AUTHTOKEN_TYPE_API_CHARACTER, vCode);
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
                            accountManager.setUserData(existingAccount, "vCode", userData.getString("vCode"));
                            accountManager.setUserData(existingAccount, "api", userData.getString("api"));
                            accountManager.setUserData(existingAccount, "characterID", userData.getString("characterID"));

                            break;
                        }
                    }

                    if (accountExists) {
                        createdAccounts.add(newAccount.name);
                    } else {
                        failedAccounts.add(newAccount.name);
                    }
                }
            }

            String toastMessage = "";
            if (!createdAccounts.isEmpty()) {
                toastMessage += String.format("Created: %1$s", TextUtils.join(", ", createdAccounts));
            }
            if (!failedAccounts.isEmpty()) {
                if (!toastMessage.isEmpty()) {
                    toastMessage += "\n";
                }
                toastMessage += String.format("Failed to create: %1$s", TextUtils.join(", ", failedAccounts));
            }
            Toast.makeText(parent, toastMessage, Toast.LENGTH_LONG).show();

            return firstAccountData;
        }
    }

}
