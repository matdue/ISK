package de.matdue.isk.eve;

import java.util.Date;

public class CacheInformation {
	
	public Date currentTime;
	public Date cachedUntil;
	
	public static String buildHashKey(String url, String... attributes) {
		StringBuilder result = new StringBuilder();
		result.append(url);
		for (String attribute : attributes) {
			result.append("-").append(attribute);
		}
		
		return result.toString();
	}

}
