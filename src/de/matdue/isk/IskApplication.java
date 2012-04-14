package de.matdue.isk;

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.IskDatabase;
import android.app.Application;

public class IskApplication extends Application {
	
	private IskDatabase iskDatabase;
	private BitmapManager bitmapManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
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
