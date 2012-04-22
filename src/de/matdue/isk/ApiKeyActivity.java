package de.matdue.isk;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import de.matdue.isk.database.ApiKey;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.eve.Account;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCacheDummy;

public class ApiKeyActivity extends Activity {
	
	private ProgressDialog waitDialog;
	private KeyCheckingTask keyCheckingTask;
	
	private static final long ACCESS_MASK = 6361217;
	private static final String CHOOSE_LINK = "https://support.eveonline.com/api/Key/ActivateInstallLinks";
	private static final String CREATE_LINK = "https://support.eveonline.com/api/Key/CreatePredefined/" + ACCESS_MASK;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.apikey);
		
		Button button = (Button) findViewById(R.id.pilots_key_button_choose);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CHOOSE_LINK));
				browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_FROM_BACKGROUND);
				startActivity(browserIntent);
			}
		});
		
		button = (Button) findViewById(R.id.pilots_key_button_create);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CREATE_LINK));
				browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_FROM_BACKGROUND);
				startActivity(browserIntent);
			}
		});
		
		button = (Button) findViewById(R.id.pilots_key_button_add);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText idField = (EditText) findViewById(R.id.pilots_key_input_id);
				String id = idField.getText().toString();
				
				EditText codeField = (EditText) findViewById(R.id.pilots_key_input_code);
				String code = codeField.getText().toString();
				
				validateKeyAndFinish(id, code);
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
			
			validateKeyAndFinish(id, code);
		}
	}
	
	private void validateKeyAndFinish(String id, String code) {
		if ("".equals(id) || "".equals(code)) {
			Toast.makeText(this, R.string.pilots_key_empty_fields, Toast.LENGTH_SHORT).show();
			return;
		}
		
		waitDialog = ProgressDialog.show(this, "", getResources().getString(R.string.pilots_key_checking), true, true, new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				keyCheckingTask.cancel(false);
				dialog.dismiss();
			}
		});
		keyCheckingTask = new KeyCheckingTask(this, waitDialog);
		keyCheckingTask.execute(id, code);
	}
	
	private static class KeyCheckingTask extends AsyncTask<String, Void, Account> {
		
		private Activity parent;
		private ProgressDialog waitDialog;
		private String id;
		private String code;
		
		public KeyCheckingTask(Activity parent, ProgressDialog waitDialog) {
			this.parent = parent;
			this.waitDialog = waitDialog;
		}

		@Override
		protected Account doInBackground(String... params) {
			id = params[0];
			code = params[1];
			
			EveApi api = new EveApi(new EveApiCacheDummy());
			Account account = api.validateKey(id, code);
			return account;
		}
		
		@Override
		protected void onPostExecute(Account result) {
			waitDialog.dismiss();
			
			if (isCancelled()) {
				return;
			}
			if (result == null) {
				Toast.makeText(parent, R.string.pilots_key_error_validate, Toast.LENGTH_LONG).show();
				return;
			}
			if ((result.accessMask & ACCESS_MASK) != ACCESS_MASK) {
				Toast.makeText(parent, R.string.pilots_key_error_accessmask, Toast.LENGTH_LONG).show();
				return;
			}
			if (!"Account".equals(result.type)) {
				Toast.makeText(parent, R.string.pilots_key_error_type, Toast.LENGTH_LONG).show();
				return;
			}
			
			storeKey(result);
			finish();
		}
		
		private void storeKey(Account account) {
			ApiKey newApiKey = new ApiKey();
			newApiKey.key = id;
			newApiKey.code = code;
			
			ArrayList<de.matdue.isk.database.Character> newCharacters = new ArrayList<de.matdue.isk.database.Character>();
			for (de.matdue.isk.eve.Character eveCharacter : account.characters) {
				de.matdue.isk.database.Character newCharacter = new de.matdue.isk.database.Character();
				newCharacter.characterId = eveCharacter.characterID;
				newCharacter.name = eveCharacter.characterName;
				newCharacter.corporationId = eveCharacter.corporationID;
				newCharacter.corporationName = eveCharacter.corporationName;
				newCharacter.selected = true;
				
				newCharacters.add(newCharacter);
			}
			
			IskDatabase iskDatabase = new IskDatabase(parent);
			iskDatabase.insertApiKey(newApiKey, newCharacters);
			iskDatabase.close();
		}
		
		private void finish() {
			Intent result = new Intent();
			result.putExtra("keyID", id);
			parent.setResult(RESULT_OK, result);
			parent.finish();
		}
		
	}

}
