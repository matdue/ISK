package de.matdue.isk;

import android.app.IntentService;
import android.content.Intent;

public abstract class WakelockedService extends IntentService {

	public WakelockedService() {
		super("de.matdue.isk.WakelockedService");
		setIntentRedelivery(false);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		WakelockHolder.getInstance()
			.prepareEnter(getApplicationContext())
			.registerClient();
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			WakelockHolder.getInstance().enter();
			
			Intent broadcastIntent = intent.getParcelableExtra("originalIntent");
			onHandleBroadcastIntent(broadcastIntent);
		} finally {
			WakelockHolder.getInstance().leave();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		WakelockHolder.getInstance().deregisterClient();
	}

	protected abstract void onHandleBroadcastIntent(Intent broadcastIntent);

}
