package de.matdue.isk.database;

class CharacterTable {
	
	// Columns
	static final String ID = "_id";
	static final String API_ID = "apiId";
	static final String CHARACTER_ID = "characterId";
	static final String NAME = "name";
	static final String CORPORATION_ID = "corporationId";
	static final String CORPORATION_NAME = "corporationName";
	static final String SELECTED = "selected";
	
	// Table
	static final String TABLE_NAME = "character";
	
	// SQL statements
	static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
		API_ID + " INTEGER," +
		CHARACTER_ID + " TEXT," +
		NAME + " TEXT," +
		CORPORATION_ID + " TEXT," +
		CORPORATION_NAME + " TEXT," +
		SELECTED + " INTEGER" +
		")";
		
	static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;

}
