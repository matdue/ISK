package de.matdue.isk.eve;

import java.math.BigDecimal;
import java.util.Date;

public class WalletJournal {
	
	// No getters and setters to achive better performance
	public Date date;
	public long refID;
	public int refTypeID;
	public String ownerName1;
	public String ownerName2;
	public String argName1;
	public BigDecimal amount;
	public BigDecimal taxAmount;
	
	public WalletTransaction transaction;

}
