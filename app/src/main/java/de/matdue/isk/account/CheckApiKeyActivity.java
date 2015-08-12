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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

import de.matdue.isk.IskActivity;
import de.matdue.isk.R;

/**
 * Activity which validates an API key using EVE Online API.
 * If a key supportes more than one characters or corporations,
 * the user has to select the desired capsuleers or corporations.
 */
public class CheckApiKeyActivity extends IskActivity implements CheckApiKeyActivityWaitFragment.CheckApiKeyCallback,
        CheckApiKeyActivityErrorFragment.CheckApiKeyErrorListener,
        CheckApiKeyActivitySelectFragment.CheckApiKeySelectListener {

    /**
     * Minimum access mask of a key
     */
    public static final int ACCESS_MASK = 6361217;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // At first, check the key using EVE Online API. As this step may take a while,
        // a please wait animation is shown. The fragment will call us back when finished.
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentById(android.R.id.content) == null) {
            String keyID = getIntent().getStringExtra("keyID");
            String vCode = getIntent().getStringExtra("vCode");
            CheckApiKeyActivityWaitFragment fragment = CheckApiKeyActivityWaitFragment.newInstance(keyID, vCode);
            fm.beginTransaction().add(android.R.id.content, fragment).commit();
        }
    }

    @Override
    public void onCheckApiKeyFinished(de.matdue.isk.eve.Account account) {
        Fragment fragment;
        if (account == null) {
            // Unknown error happened, probably communication error
            fragment = CheckApiKeyActivityErrorFragment.newInstance(R.string.pilots_key_error_validate);
        } else if ((account.accessMask & ACCESS_MASK) != ACCESS_MASK) {
            // API key does not match the required access mask, i.e. provided not enough rights
            fragment = CheckApiKeyActivityErrorFragment.newInstance(R.string.pilots_key_error_accessmask);
        } else if (account.characters.isEmpty()) {
            // API key does not have any capsuleers
            fragment = CheckApiKeyActivityErrorFragment.newInstance(R.string.account_checkapi_no_pilots);
        } else {
            // API Key supports one or more capsuleers

            // Check for required name
            String requiredAccountName = getIntent().getStringExtra("name");
            if (requiredAccountName != null) {
                // The API key for a specific name was requested (probably because the token is invalid).
                de.matdue.isk.eve.Character requiredCharacter = null;
                for (de.matdue.isk.eve.Character character : account.characters) {
                    if (requiredAccountName.equals(account.isCorporation() ? character.corporationName : character.characterName)) {
                        requiredCharacter = character;
                        break;
                    }
                }

                if (requiredCharacter == null) {
                    String errorMessage = getString(R.string.account_checkapi_missing_pilot, requiredAccountName);
                    fragment = CheckApiKeyActivityErrorFragment.newInstance(errorMessage);
                } else {
                    // Select required capsuleer and skip selection
                    account.characters = new ArrayList<>();
                    account.characters.add(requiredCharacter);
                    onSelectionFinished(account);
                    return;
                }
            } else {
                // If there is one capsuleer only, skip selection
                if (account.characters.size() == 1) {
                    onSelectionFinished(account);
                    return;
                }

                // else show selection fragment
                fragment = CheckApiKeyActivitySelectFragment.newInstance(account);
            }
        }

        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    @Override
    public void onSelectionFinished(de.matdue.isk.eve.Account account) {
        if (account == null) {
            setResult(Activity.RESULT_CANCELED);
        } else {
            Intent resultIntent = new Intent();
            resultIntent.putExtras(getIntent());
            resultIntent.putExtra("account", account);
            setResult(Activity.RESULT_OK, resultIntent);
        }
        finish();
    }

    @Override
    public void onErrorFinished() {
        finish();
    }
}