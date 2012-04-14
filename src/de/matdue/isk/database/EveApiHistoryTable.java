package de.matdue.isk.database;

class EveApiHistoryTable {
	
	// Columns
	static final String URL = "url";
	static final String KEY_ID = "keyId";
	static final String TIMESTAMP = "timestamp";
	static final String RESULT = "result";
	
	// Table
	static final String TABLE_NAME = "eveApiHistory";
	
	// SQL statements
	static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		URL + " TEXT," +
		KEY_ID + " TEXT," +
		TIMESTAMP + " INTEGER," +
		RESULT + " TEXT" +
		")";
	
	static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
