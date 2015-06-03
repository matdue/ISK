package de.matdue.isk.account;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Matthias on 10.02.2015.
 */
public class IskAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        AccountAuthenticator authenticator = new AccountAuthenticator(this);
        return authenticator.getIBinder();
    }
}
