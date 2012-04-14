package de.matdue.isk.database;

import java.math.BigDecimal;
import java.util.Date;

public class Wallet {

	// No getters and setters to achive better performance
	public Date date;
	public int refTypeID;
	public String ownerName1;
	public String ownerName2;
	public BigDecimal amount;
	public BigDecimal taxAmount;
	
	public int quantity;
	public String typeName;
	public String typeID;
	public BigDecimal price;
	public String clientName;
	public String stationName;
	public String transactionType;
	public String transactionFor;
		
}
