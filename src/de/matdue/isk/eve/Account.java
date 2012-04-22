package de.matdue.isk.eve;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Account {
	
	// No getters and setters to achive better performance
	public long accessMask;
	public String type;
	public Date expires;
	public List<Character> characters;
	
	public Account() {
		characters = new ArrayList<Character>();
	}

}
