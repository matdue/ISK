/**
 * Copyright 2015 Matthias Düsterhöft
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
package de.matdue.isk.account;

/**
 * API key data got from an authentication token
 */
public class ApiKey {

    private String keyID;
    private String vCode;

    public ApiKey(String token) {
        String[] data = token.split("\\|");
        keyID = data[0];
        vCode = data[1];
    }

    public String getKeyID() {
        return keyID;
    }

    public String getVCode() {
        return vCode;
    }

    @Override
    public String toString() {
        return "keyID=" + keyID + ", vCode=" + vCode;
    }
}
