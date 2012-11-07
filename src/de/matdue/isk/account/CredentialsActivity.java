package de.matdue.isk.account;

import de.matdue.isk.MainActivity;
import de.matdue.isk.R;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CredentialsActivity extends AccountAuthenticatorActivity {
	
	private static final long ACCESS_MASK = 6295553;
	private static final String CHOOSE_LINK = "https://support.eveonline.com/api/Key/ActivateInstallLinks";
	private static final String CREATE_LINK = "https://support.eveonline.com/api/Key/CreatePredefined/" + ACCESS_MASK;
	
	private static final int ApiKeyFinishActivityRequestCode = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.credentials);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
            
        default:
    		return super.onOptionsItemSelected(item);
		}
	}
	
	public void onAddClicked(View view) {
		EditText idField = (EditText) findViewById(R.id.pilots_key_input_id);
		String keyId = idField.getText().toString();
		
		EditText codeField = (EditText) findViewById(R.id.pilots_key_input_code);
		String vCode = codeField.getText().toString();
		
		Intent intent = new Intent(this, ApiKeyFinishActivity.class);
		intent.putExtra(Constants.PARAM_KEYID, keyId);
		intent.putExtra(Constants.PARAM_VCODE, vCode);
		startActivityForResult(intent, ApiKeyFinishActivityRequestCode);
	}
	
	public void onChooseClicked(View view) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CHOOSE_LINK));
		browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivity(browserIntent);
	}
	
	public void onCreateClicked(View view) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CREATE_LINK));
		browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivity(browserIntent);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ApiKeyFinishActivityRequestCode && resultCode == RESULT_OK) {
			String characterName = data.getStringExtra(Constants.PARAM_CHAR_NAME);
			
			Bundle newAccountData = new Bundle();
			newAccountData.putString("characterID", data.getStringExtra(Constants.PARAM_CHAR_ID));
			newAccountData.putString("keyID", data.getStringExtra(Constants.PARAM_KEYID));
			newAccountData.putString("vCode", data.getStringExtra(Constants.PARAM_VCODE));
			
			AccountManager accountManager = AccountManager.get(this);
			Account newAccount = new Account(characterName, Constants.ACCOUNT_TYPE);
			if (!accountManager.addAccountExplicitly(newAccount, null, newAccountData)) {
				Toast.makeText(this, String.format("Das Konto für %1$s konnte nicht erstellt werden, da es bereits existiert.", characterName), Toast.LENGTH_LONG).show();
				return;
			}
			Toast.makeText(this, String.format("Konto für %1$s wurde erstellt.", characterName), Toast.LENGTH_LONG).show();
			
			Intent intent = new Intent();
	        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, characterName);
	        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
	        setAccountAuthenticatorResult(intent.getExtras());
	        setResult(RESULT_OK, intent);
	        finish();
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith("eve://api.eveonline.com/installKey")) {
			String keyId = uri.getQueryParameter("keyID");
			String vCode = uri.getQueryParameter("vCode");
			
			Intent finishIntent = new Intent(this, ApiKeyFinishActivity.class);
			finishIntent.putExtra(Constants.PARAM_KEYID, keyId);
			finishIntent.putExtra(Constants.PARAM_VCODE, vCode);
			startActivityForResult(finishIntent, ApiKeyFinishActivityRequestCode);
		}
	}
	
}
