package de.matdue.isk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class WakelockedBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Keep CPU alive
		WakelockHolder.getInstance().prepareEnter(context);
		
		// Start service
		Intent serviceIntent = new Intent(context, getServiceClass());
		serviceIntent.putExtra("originalIntent", intent);
		context.startService(serviceIntent);
	}
	
	protected abstract Class<? extends WakelockedService> getServiceClass();

}
