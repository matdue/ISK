package de.matdue.isk.sync;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	
	private Context context;
	private AccountManager accountManager;
	
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.context = context;
		this.accountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.v("SyncAdapter", "onPerformSync");
		try {
			String authToken = accountManager.blockingGetAuthToken(account, "", true);
			syncResult.stats.numUpdates++;
		} catch (OperationCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			syncResult.stats.numAuthExceptions++;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			syncResult.stats.numIoExceptions++;
		}
	}

}
