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
package de.matdue.isk.eve;

import java.math.BigDecimal;
import java.util.Date;

public class WalletTransaction {
	
	// No getters and setters to achive better performance
	public Date date;
	public long transactionID;
	public int quantity;
	public String typeName;
	public String typeID;
	public BigDecimal price;
	public String clientName;
	public String stationName;
	public String transactionType;
	public String transactionFor;
	public long journalTransactionID;

}
