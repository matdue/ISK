package de.matdue.isk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ApiCallbackActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent nextIntent = new Intent(this, ApiKeyActivity.class);
		// ApiKeyActivity should become top activity
		nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		// Forward return data
		nextIntent.setData(getIntent().getData());
		startActivity(nextIntent);
		finish();
	}

}
