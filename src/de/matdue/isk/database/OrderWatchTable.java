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

class OrderWatchTable {

	// Columns
	static final String CHARACTER_ID = "characterId";
	static final String ORDER_ID = "orderID";
	static final String ORDER_STATE = "orderState";
	static final String TYPE_ID = "typeID";
	static final String TYPE_NAME = "typeName";
	static final String STATION_ID = "stationID";
	static final String STATION_NAME = "stationName";
	static final String PRICE = "price";
	static final String VOL_ENTERED = "volEntered";
	static final String VOL_REMAINING = "volRemaining";
	static final String FULFILLED = "fulfilled";
	static final String EXPIRES = "expires";
	static final String ACTION = "action";
	static final String STATUS = "status";
	static final String SORT_KEY = "sortKey";
	static final String SEQ_ID = "seqId";
	
	// Table
	static final String TABLE_NAME = "orderWatch";

	// SQL statements
	static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		CHARACTER_ID + " TEXT," +
		ORDER_ID + " INTEGER," +
		ORDER_STATE + " INTEGER," +
		TYPE_ID + " INTEGER," +
		TYPE_NAME + " TEXT," +
		STATION_ID + " INTEGER," +
		STATION_NAME + " TEXT," +
		PRICE + " TEXT," +
		VOL_ENTERED + " INTEGER," +
		VOL_REMAINING + " INTEGER," +
		FULFILLED + " INTEGER," +
		EXPIRES + " INTEGER," +
		ACTION + " INTEGER," +
		STATUS + " INTEGER," +
		SORT_KEY + " INTEGER," +
		SEQ_ID + " INTEGER" +
		")";
	
	static final String SQL_DROP = 
		"DROP TABLE IF EXISTS " + TABLE_NAME;
		
}
