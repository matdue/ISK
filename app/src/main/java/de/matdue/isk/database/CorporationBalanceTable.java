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

public class CorporationBalanceTable {

    // Columns
    static final String CORPORATION_ID = "corporationId";
    static final String BALANCE = "balance";
    static final String ACCOUNT_KEY = "accountKey";
    static final String DESCRIPTION = "description";

    // Table
    static final String TABLE_NAME = "corporationBalance";

    // SQL statements
    static final String SQL_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    CORPORATION_ID + " TEXT," +
                    BALANCE + " TEXT," +
                    ACCOUNT_KEY + " TEXT," +
                    DESCRIPTION + " TEXT" +
                    ")";

    static final String SQL_DROP =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    static final String STMT_CLEAR =
            "DELETE FROM " + TABLE_NAME;

}
