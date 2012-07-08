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

import java.math.BigDecimal;
import java.util.Date;

public class OrderWatch {
	
	public static final int WATCH = 0x00000001;

	// No getters and setters to achive better performance
	public String characterId;
	public long orderID;  // 0 means inactive order
	public int orderState;
	public int typeID;
	public String typeName;
	public int stationID;
	public String stationName;
	public BigDecimal price;
	public int volEntered;
	public int volRemaining;
	public int fulfilled;  // 100 = 100% fulfilled, i.e. volRemaining=0, .., 0 = 0% fulfilled, i.e. volRemaining=volEntered
	public Date expires;
	public int action;  // 0 = sell, 1 = buy
	public int status;
	public int sortKey;  // Used for sorting: inactive orders first, then most fulfilled to least fulfilled
	public long seqId;  // unique ID, in combination with characterId
	
}
