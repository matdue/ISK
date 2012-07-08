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
package de.matdue.isk.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class EveDatabase extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "eveInferno.db";
	private static final int DATABASE_VERSION = 1;
	
	private Context context;
	
	public EveDatabase(Context context) {
		super(context, context.getExternalFilesDir(null).getAbsolutePath() + "/" + DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	@Override
	public synchronized SQLiteDatabase getReadableDatabase() {
		checkDatabase();
		return super.getReadableDatabase();
	}
	
	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		checkDatabase();
		return super.getWritableDatabase();
	}
	
	private void checkDatabase() {
		// Check environment
		String storageState = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(storageState) && !(Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState))) {
			// Not mounted => database file will not exists, and can't be copied to external storage
			return;
		}
		
		// Check if file exists
		File externalFilesDir = context.getExternalFilesDir(null);
		File databaseFile = new File(externalFilesDir, DATABASE_NAME);
		if (databaseFile.canRead()) {
			return;
		}
		
		// Remove old files
		File[] oldDatabaseFiles = externalFilesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".db");
			}
		});
		for (File oldDatabaseFile : oldDatabaseFiles) {
			oldDatabaseFile.delete();
		}
		
		// Copy file from assets
		try {
			InputStream dbInputStream = context.getAssets().open("database/" + DATABASE_NAME);
			OutputStream dbOutputStream = new FileOutputStream(databaseFile);
			
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dbInputStream.read(buffer)) > 0) {
				dbOutputStream.write(buffer, 0, bytesRead);
			}
			
			dbOutputStream.flush();
			dbOutputStream.close();
			dbInputStream.close();
		} catch (IOException e) {
			Log.e(EveDatabase.class.toString(), "Error copying database assets/database/" + DATABASE_NAME + " -> " + databaseFile.getAbsolutePath(), e);
		}
	}
	
	public String queryTypeName(int typeID) {
		String result = null;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(EveInvTypesTable.TABLE_NAME, 
					new String[] { EveInvTypesTable.TYPE_NAME }, 
					EveInvTypesTable.TYPE_ID + "=?", 
					new String[] { Integer.toString(typeID) }, 
					null, 
					null, 
					null);
			if (cursor.moveToNext()) {
				result = cursor.getString(0);
			}
			cursor.close();
		} catch (SQLiteException e) {
			Log.e("EveDatabase", "queryTypeName", e);
		}
		
		return result;
	}
	
	public String queryStationName(int stationID) {
		String result = null;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(EveStaStationsTable.TABLE_NAME, 
					new String[] { EveStaStationsTable.STATION_NAME }, 
					EveStaStationsTable.STATION_ID + "=?", 
					new String[] { Integer.toString(stationID) }, 
					null, 
					null, 
					null);
			if (cursor.moveToNext()) {
				result = cursor.getString(0);
			}
			cursor.close();
		} catch (SQLiteException e) {
			Log.e("EveDatabase", "queryStationName", e);
		}
		
		return result;
	}

}
