/**
 * Copyright 2016 Matthias Düsterhöft
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

/**
 * Table data for type cache
 */
public class EveTypeTable {

    // Columns
    static final String ID = "id";
    static final String NAME = "name";
    static final String CREATED = "created";

    // Table
    static final String TABLE_NAME = "eveType";

    // SQL statements
    static final String SQL_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    ID + " TEXT," +
                    NAME + " TEXT," +
                    CREATED + " INTEGER" +
                    ")";

    static final String IDX_CREATE =
            "CREATE INDEX Idx" + TABLE_NAME + "Id ON " + TABLE_NAME + "(" +
                    ID +
                    ")"
            ;

    static final String SQL_DROP =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    static final String IDX_DROP =
            "DROP INDEX IF EXISTS Idx" + TABLE_NAME + "Id";

    static final String STMT_CLEAR =
            "DELETE FROM " + TABLE_NAME;

}
