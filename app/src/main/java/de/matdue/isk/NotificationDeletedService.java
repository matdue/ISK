/**
 * Copyright 2012 Matthias Düsterhöft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.matdue.isk;

import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.database.OrderWatch;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class NotificationDeletedService extends IntentService {

	public NotificationDeletedService() {
		super("NotificationDeletedService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			Intent broadcastIntent = intent.getParcelableExtra("originalIntent");
			String characterId = broadcastIntent.getStringExtra("characterID");
			IskApplication iskApplication = (IskApplication) getApplication();
			IskDatabase iskDatabase = iskApplication.getIskDatabase();
			iskDatabase.setOrderWatchStatusBits(characterId, OrderWatch.NOTIFIED_AND_READ);
		} catch (Exception e) {
			Log.e("NotificationDeleted",  "Error occurred", e);
		} finally {
			NotificationDeletedReceiver.completeWakefulIntent(intent);
		}
	}
}
