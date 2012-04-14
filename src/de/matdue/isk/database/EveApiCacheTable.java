package de.matdue.isk.database;

class EveApiCacheTable {

	// Columns
	static final String KEY = "key";
	static final String CACHED_UNTIL = "cachedUntil";
	
	// Table
	static final String TABLE_NAME = "eveApiCache";
	
	// SQL statements
	static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		KEY + " TEXT," +
		CACHED_UNTIL + " INTEGER" +
		")";
		
	static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;
	
}
