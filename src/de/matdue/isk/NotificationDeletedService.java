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
import android.content.Intent;
import android.util.Log;

public class NotificationDeletedService extends WakelockedService {

	@Override
	protected void onHandleBroadcastIntent(Intent broadcastIntent) {
		try {
			IskApplication iskApplication = (IskApplication) getApplication();
			IskDatabase iskDatabase = iskApplication.getIskDatabase();
			iskDatabase.setOrderWatchStatusBits(OrderWatch.NOTIFIED_AND_READ);
		} catch (Exception e) {
			Log.e("NotificationDeletedService",  "Error occured", e);
		}
	}

}
