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

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.EveDatabase;
import de.matdue.isk.database.IskDatabase;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;

public class IskApplication extends Application {
	
	private IskDatabase iskDatabase;
	private EveDatabase eveDatabase;
	private BitmapManager bitmapManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		// Enable strict mode
		int applicationFlags = getApplicationInfo().flags;
		if ((applicationFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectDiskReads()
					.detectDiskWrites()
					.detectNetwork()
					.penaltyLog()
					.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedClosableObjects()
					.detectLeakedSqlLiteObjects()
					.detectActivityLeaks()
					.penaltyLog()
					.build());
		}
		
		iskDatabase = new IskDatabase(this);
		eveDatabase = new EveDatabase(this);
		bitmapManager = new BitmapManager(this, getCacheDir());
	}
	
	public IskDatabase getIskDatabase() {
		return iskDatabase;
	}
	
	public EveDatabase getEveDatabase() {
		return eveDatabase;
	}
	
	public BitmapManager getBitmapManager() {
		return bitmapManager;
	}

}
