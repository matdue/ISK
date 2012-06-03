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
