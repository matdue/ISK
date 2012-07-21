package de.matdue.isk;

import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.database.OrderWatch;
import android.content.Intent;
import android.util.Log;

public class NotificationDeletedService extends WakelockedService {

	@Override
	protected void onHandleBroadcastIntent(Intent broadcastIntent) {
		IskDatabase iskDatabase = null;
		try {
			iskDatabase = new IskDatabase(getApplicationContext());
			iskDatabase.setOrderWatchStatusBits(OrderWatch.NOTIFIED);
		} catch (Exception e) {
			Log.e("NotificationDeletedService",  "Error occured", e);
		} finally {
			if (iskDatabase != null) {
				iskDatabase.close();
				iskDatabase = null;
			}
		}
	}

}
