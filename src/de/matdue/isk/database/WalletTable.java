package de.matdue.isk.database;

class WalletTable {

	// Columns
	static final String CHARACTER_ID = "characterId";
	static final String DATE = "date";
	static final String REFTYPEID = "refTypeID";
	static final String OWNERNAME1 = "ownerName1";
	static final String OWNERNAME2 = "ownerName2";
	static final String AMOUNT = "amount";
	static final String TAX_AMOUNT = "taxAmount";
	static final String QUANTITY = "quantity";
	static final String TYPE_NAME = "typeName";
	static final String TYPE_ID = "typeID";
	static final String PRICE = "price";
	static final String CLIENT_NAME = "clientName";
	static final String STATION_NAME = "stationName";
	static final String TRANSACTION_TYPE = "transactionType";
	static final String TRANSACTION_FOR = "transactionFor";
	
	// Table
	static final String TABLE_NAME = "wallet";
	
	// SQL statements
	static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		CHARACTER_ID + " TEXT," +
		DATE + " INTEGER," +
		REFTYPEID + " INTEGER," +
		OWNERNAME1 + " TEXT," +
		OWNERNAME2 + " TEXT," +
		AMOUNT + " TEXT," +
		TAX_AMOUNT + " TEXT," +
		QUANTITY + " INTEGER," +
		TYPE_NAME + " TEXT," +
		TYPE_ID + " TEXT," +
		PRICE + " TEXT," +
		CLIENT_NAME + " TEXT," +
		STATION_NAME + " TEXT," +
		TRANSACTION_TYPE + " TEXT," +
		TRANSACTION_FOR + " TEXT" +
		")";
	
	static final String IDX_CREATE =
		"CREATE INDEX Idx" + TABLE_NAME + "CharDate ON " + TABLE_NAME + "(" +
		CHARACTER_ID + "," + DATE +
		")"
		;
	
	static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
	
	static final String IDX_DROP =
		"DROP INDEX IF EXISTS Idx" + TABLE_NAME + "CharDate";
	
	static final String STMT_CLEAR = 
		"DELETE FROM " + TABLE_NAME;
	
}
