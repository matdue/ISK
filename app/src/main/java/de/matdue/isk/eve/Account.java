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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Account implements Serializable {
	
	// No getters and setters to achive better performance
	public long accessMask;
	public String type;
	public Date expires;
	public List<Character> characters;
	public String errorCode;
	public String errorText;
	
	public Account() {
		characters = new ArrayList<Character>();
	}

	/**
	 * Checks whether this account is a corporation or a character account.
	 *
	 * @return <code>true</code> for corporation account, else <code>false</code>
	 */
	public boolean isCorporation() {
		return "Corporation".equals(type);
	}

}
