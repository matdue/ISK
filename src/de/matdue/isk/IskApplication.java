package de.matdue.isk;

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.IskDatabase;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;

public class IskApplication extends Application {
	
	private IskDatabase iskDatabase;
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
		bitmapManager = new BitmapManager(this, getCacheDir());
	}
	
	public IskDatabase getIskDatabase() {
		return iskDatabase;
	}
	
	public BitmapManager getBitmapManager() {
		return bitmapManager;
	}

}
