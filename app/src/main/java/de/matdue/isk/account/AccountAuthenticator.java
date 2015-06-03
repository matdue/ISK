package de.matdue.isk.account;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Matthias on 09.02.2015.
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    /**
     * Account type id
     */
    public static final String ACCOUNT_TYPE = "de.matdue.isk";

    /**
     * Auth token types
     */
    public static final String AUTHTOKEN_TYPE_API_CHARACTER = "API Character";
    public static final String AUTHTOKEN_TYPE_API_CORPORATION = "API Corporation";
    public static final String AUTHTOKEN_TYPE_CREST = "CREST";

    private Context context;

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.v("AccountAuthenticator", "addAccount");

        Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.v("AccountAuthenticator", "getAuthToken");

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!AUTHTOKEN_TYPE_API_CHARACTER.equals(authTokenType) &&
                !AUTHTOKEN_TYPE_API_CORPORATION.equals(authTokenType) &&
                !AUTHTOKEN_TYPE_CREST.equals(authTokenType)) {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        AccountManager accountManager = AccountManager.get(context);
        String authToken = accountManager.peekAuthToken(account, authTokenType);
        Log.v("AccountAuthenticator", "peekAuthToken returned - " + authToken);

        // Try to get new token using the refresh token
        if (TextUtils.isEmpty(authToken) && AUTHTOKEN_TYPE_CREST.equals(authTokenType)) {
            // TODO: implement!
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_NAME, account.name);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (AUTHTOKEN_TYPE_API_CHARACTER.equals(authTokenType))
            return AUTHTOKEN_TYPE_API_CHARACTER;  // TODO: l10n
        else if (AUTHTOKEN_TYPE_API_CORPORATION.equals(authTokenType))
            return AUTHTOKEN_TYPE_API_CORPORATION;
        else if (AUTHTOKEN_TYPE_CREST.equals(authTokenType))
            return AUTHTOKEN_TYPE_CREST;

        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }
}
