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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.matdue.isk.R;
import de.matdue.isk.eve.Account;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCacheDummy;

/**
 * Fragment to show a wait animation.
 * It calls {@link CheckApiKeyCallback#onCheckApiKeyFinished(Account)} when finished.
 */
public class CheckApiKeyActivityWaitFragment extends Fragment {

    /**
     * The fragment's activity must implement this interface.
     */
    public interface CheckApiKeyCallback {
        /**
         * Fragment calls this method as soon as the API key check has finished.
         *
         * @param account Account data, got from EVE Online API
         */
        void onCheckApiKeyFinished(de.matdue.isk.eve.Account account);
    }

    private CheckApiKeyCallback callback;

    public static CheckApiKeyActivityWaitFragment newInstance(String keyID, String vCode) {
        Bundle arguments = new Bundle();
        arguments.putString("keyID", keyID);
        arguments.putString("vCode", vCode);

        CheckApiKeyActivityWaitFragment newFragment = new CheckApiKeyActivityWaitFragment();
        newFragment.setArguments(arguments);
        return newFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_check_api_key_wait, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not recreate this fragment when a configuration change (or screen rotation) happened.
        // Otherwise, onCreate() would be called again and we would start the background task again.
        setRetainInstance(true);

        // Start task to check API key in background
        // Don't use the default executor because only one async task will be executed then.
        // This would be a race condition if below task would be called from another async task.
        new AsyncTask<Bundle, Void, de.matdue.isk.eve.Account>() {
            @Override
            protected de.matdue.isk.eve.Account doInBackground(Bundle... params) {
                String id = params[0].getString("keyID");
                String vCode = params[0].getString("vCode");
                EveApi api = new EveApi(new EveApiCacheDummy());
                return api.validateKey(id, vCode);
            }

            @Override
            protected void onPostExecute(de.matdue.isk.eve.Account account) {
                // Notify callback about finished work
                if (callback != null) {
                    callback.onCheckApiKeyFinished(account);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getArguments());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callback = (CheckApiKeyCallback) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}
