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
package de.matdue.isk.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * {@link android.widget.ListAdapter} implementation which accepts a list of items
 * and a resource ID for the view, which will display a single item.

 * This adapter supports ListViews with single or multiple choices.
 *
 * <code>T</code> is the type of an item.
 *
 * @author Matthias Düsterhöft
 */
public abstract class ResourceListAdapter<T> extends BaseAdapter {

    /**
     * List of items.
     */
    private List<T> items;

    /**
     * Inflater to use for creating the view
     */
    private LayoutInflater inflater;

    /**
     * Context
     */
    private Context context;

    /**
     * Layout ID of the view
     */
    private int layout;

    /**
     * @param context    Context
     * @param layout     Layout ID for creating the view
     * @param items      List of items
     */
    public ResourceListAdapter(Context context, int layout, List<T> items) {
        this.items = items;
        this.context = context;
        this.layout = layout;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(layout, parent, false);
        }

        bindView(view, context, items.get(position), ((ListView) parent).isItemChecked(position));
        return view;
    }

    /**
     * Binds data of the item to the view.
     *
     * @param view       View
     * @param context    Context
     * @param item       Item which should be displayed
     * @param checked    Indicates if the item is selected in the ListView; always <code>false</code> if ListView is not in choice mode
     */
    public abstract void bindView(View view, Context context, T item, boolean checked);
}