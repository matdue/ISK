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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.matdue.isk.IskActivity;
import de.matdue.isk.R;
import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.eve.*;
import de.matdue.isk.widget.ResourceListAdapter;

/**
 * Fragment to allow the user to selected the desired pilots or corporations.
 */
public class CheckApiKeyActivitySelectFragment extends ListFragment {

    private Account account;

    /**
     * The fragment's activity must implement this interface.
     */
    public interface CheckApiKeySelectListener {
        /**
         * Fragment calls this method when the user clicked the cancel button
         */
        /**
         * Fragment calls this method when the user clicked the create or cancel button
         * @param account EVE Online account data with selected pilots or corporations, or <code>null</code> if cancelled
         */
        void onSelectionFinished(de.matdue.isk.eve.Account account);
    }

    private CheckApiKeySelectListener callback;

    /**
     * Creates fragments.
     *
     * @param account EVE Online account data
     * @return New fragment
     */
    public static CheckApiKeyActivitySelectFragment newInstance(de.matdue.isk.eve.Account account) {
        Bundle args = new Bundle();
        args.putSerializable("account", account);

        CheckApiKeyActivitySelectFragment newFragment = new CheckApiKeyActivitySelectFragment();
        newFragment.setArguments(args);
        return newFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        account = (Account) getArguments().get("account");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_check_api_key_select, container, false);

        Button button = (Button) view.findViewById(R.id.fragment_check_api_key_button_cancel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onSelectionFinished(null);
                }
            }
        });

        button = (Button) view.findViewById(R.id.fragment_check_api_key_button_create);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    ArrayList<de.matdue.isk.eve.Character> selectedCharacters = new ArrayList<>();

                    // Check which items have been selected
                    ListView listView = getListView();
                    SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
                    for (int i = 0; i < checkedItemPositions.size(); ++i) {
                        if (checkedItemPositions.valueAt(i)) {
                            selectedCharacters.add(account.characters.get(checkedItemPositions.keyAt(i)));
                        }
                    }

                    account.characters = selectedCharacters;
                    callback.onSelectionFinished(account);
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ArrayList<SelectedCharacter> selectedCharacters = new ArrayList<>(account.characters.size());
        for (de.matdue.isk.eve.Character character : account.characters) {
            SelectedCharacter selectedCharacter = new SelectedCharacter();
            selectedCharacter.eveCharacter = character;
            selectedCharacter.corporation = account.isCorporation();

            selectedCharacters.add(selectedCharacter);
        }

        SelectCharacterAdapter adapter = new SelectCharacterAdapter(getActivity(), ((IskActivity) getActivity()).getBitmapManager(), selectedCharacters);
        setListAdapter(adapter);

        ListView listView = getListView();
        for (int i = 0; i < selectedCharacters.size(); ++i) {
            listView.setItemChecked(i, true);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callback = (CheckApiKeySelectListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    /**
     * Model of a single pilot or corporation
     */
    public static class SelectedCharacter {
        public de.matdue.isk.eve.Character eveCharacter;
        public boolean corporation;
    }

    private static class SelectCharacterAdapter extends ResourceListAdapter<SelectedCharacter> {
        private final BitmapManager bitmapManager;

        public SelectCharacterAdapter(Context context, BitmapManager bitmapManager, List<SelectedCharacter> selectedCharacters) {
            super(context, R.layout.fragment_check_api_key_select_item, selectedCharacters);

            this.bitmapManager = bitmapManager;
        }

        static class ViewHolder {
            ImageView image;
            CheckedTextView text;
        }

        @Override
        public void bindView(View view, Context context, SelectedCharacter item, boolean checked) {
            // ViewHolder pattern
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder();
                viewHolder.image = (ImageView) view.findViewById(R.id.fragment_check_api_key_select_item_image);
                viewHolder.text = (CheckedTextView) view.findViewById(R.id.fragment_check_api_key_select_item_text);

                view.setTag(viewHolder);
            }

            int resolution = calculateResolution(viewHolder.image);
            bitmapManager.setImageBitmap(viewHolder.image,
                    item.corporation ? EveApi.getCorporationUrl(item.eveCharacter.corporationID, resolution) : EveApi.getCharacterUrl(item.eveCharacter.characterID, resolution),
                    null, null);

            viewHolder.text.setText(item.corporation ? item.eveCharacter.corporationName : item.eveCharacter.characterName);
            viewHolder.text.setChecked(checked);
        }

        /**
         * Calculate appropriate resolution: Image should be downsized if needed,
         * but on the other hand download the smallest resolution.
         *
         * @param imageView ImageView which will receive the image
         * @return 128 or 256
         */
        private int calculateResolution(ImageView imageView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // ImageView.getMaxWidth() is not available before Jelly Bean
                int maxWidth = imageView.getMaxWidth();
                return maxWidth < 128 ? 128 : 256;
            } else {
                return 128;
            }
        }
    }
}
