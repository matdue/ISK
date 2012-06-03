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
