package de.matdue.isk.account;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by Matthias on 15.02.2015.
 */
public class ApiCallbackActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent nextIntent = new Intent(this, AuthenticatorActivity.class);
        // Forward return data
        nextIntent.setData(getIntent().getData());
        startActivity(nextIntent);
        finish();
    }

}
