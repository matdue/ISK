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
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import de.matdue.isk.R;

/**
 * Fragment to show a dynamic error message.
 * It calls {@link CheckApiKeyErrorListener#onErrorFinished()} when finished.
 */
public class CheckApiKeyActivityErrorFragment extends Fragment {

    /**
     * The fragment's activity must implement this interface.
     */
    public interface CheckApiKeyErrorListener {
        /**
         * Fragment calls this method when the user clicked the cancel button
         */
        void onErrorFinished();
    }

    private CheckApiKeyErrorListener callback;

    /**
     * Creates fragment.
     *
     * @param errorMessageId Resource ID of error message
     * @return New fragment
     */
    public static CheckApiKeyActivityErrorFragment newInstance(int errorMessageId) {
        Bundle args = new Bundle();
        args.putInt("errorMessageId", errorMessageId);

        CheckApiKeyActivityErrorFragment newFragment = new CheckApiKeyActivityErrorFragment();
        newFragment.setArguments(args);
        return newFragment;
    }

    /**
     * Creates fragment.
     *
     * @param errorMessage Error message
     * @return New fragment
     */
    public static CheckApiKeyActivityErrorFragment newInstance(String errorMessage) {
        Bundle args = new Bundle();
        args.putString("errorMessage", errorMessage);

        CheckApiKeyActivityErrorFragment newFragment = new CheckApiKeyActivityErrorFragment();
        newFragment.setArguments(args);
        return newFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_check_api_key_error, container, false);

        Button button = (Button) view.findViewById(R.id.fragment_check_api_key_button_cancel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onErrorFinished();
                }
            }
        });

        TextView textView = (TextView) view.findViewById(R.id.fragment_check_api_key_error_text);
        int errorMessageId = getArguments().getInt("errorMessageId");
        if (errorMessageId != 0) {
            textView.setText(errorMessageId);
        } else {
            textView.setText(getArguments().getString("errorMessage"));
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callback = (CheckApiKeyErrorListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}
