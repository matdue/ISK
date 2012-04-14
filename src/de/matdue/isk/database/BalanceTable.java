package de.matdue.isk.database;

class BalanceTable {
	
	// Columns
	static final String CHARACTER_ID = "characterId";
	static final String BALANCE = "balance";
	
	// Table
	static final String TABLE_NAME = "balance";
	
	// SQL statements
	static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		CHARACTER_ID + " TEXT," +
		BALANCE + " TEXT" +
		")";
		
	static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
