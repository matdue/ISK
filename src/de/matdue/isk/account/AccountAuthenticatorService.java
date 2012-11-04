package de.matdue.isk.account;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AccountAuthenticatorService extends Service {
	
	private AccountAuthenticator authenticator;
	
	@Override
	public void onCreate() {
		authenticator = new AccountAuthenticator(this);
	}
	
	@Override
	public void onDestroy() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return authenticator.getIBinder();
	}

}
