package de.matdue.isk.account;

import java.util.List;

import de.matdue.isk.eve.Character;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCacheDummy;
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AccountAuthenticator extends AbstractAccountAuthenticator {
	
	private Context context;

	public AccountAuthenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response,
			String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options)
			throws NetworkErrorException {
		Log.v("AccountAuthenticator", "addAccount");
		Intent intent = new Intent(context, CredentialsActivity.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,
			Account account, Bundle options) throws NetworkErrorException {
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,
			String accountType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		Log.v("AccountAuthenticator", "getAuthToken");
		
		// Check if current credentials are valid
		AccountManager accountManager = AccountManager.get(context);
		String characterID = accountManager.getUserData(account, Constants.ACCOUNT_CHARACTER_ID);
		String keyID = accountManager.getUserData(account, Constants.ACCOUNT_KEY_ID);
		String vCode = accountManager.getUserData(account, Constants.ACCOUNT_V_CODE);
		
		EveApi api = new EveApi(new EveApiCacheDummy());
		List<Character> characters = api.queryCharacters(keyID, vCode);
		api.close();
		
		if (characters != null && characterID != null) {
			for (Character character : characters) {
				if (characterID.equals(character.characterID) && account.name.equals(character.characterName)) {
					String authToken = characterID + "|" + keyID + "|" + vCode;
					
					Bundle result = new Bundle();
			        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			        result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
			        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
			        return result;
				}
			}
		}
		
		// Credentials not valid
		Intent intent = new Intent(context, CredentialsActivity.class);
		intent.putExtra(Constants.PARAM_ACCOUNTNAME, account.name);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		// null means we don't support multiple authToken types
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,
			Account account, String[] features) throws NetworkErrorException {
		// This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
		Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		return null;
	}

}
