package de.matdue.isk.database;

class ApiKeyTable {
	
	// Columns
	static final String ID = "_id";
	static final String KEY = "key";
	static final String CODE = "code";
	
	// Table
	static final String TABLE_NAME = "apiKey";
	
	// SQL statements
	static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
		KEY + " TEXT," +
		CODE + " TEXT" +
		")";
	
	static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
