package de.matdue.isk;

import de.matdue.isk.database.IskDatabase;
import android.app.Application;

public class IskApplication extends Application {
	
	private IskDatabase iskDatabase;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		iskDatabase = new IskDatabase(this);
	}
	
	public IskDatabase getIskDatabase() {
		return iskDatabase;
	}

}
