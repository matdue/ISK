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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * LinearLayout derivation which implements the {@link android.widget.Checkable} interface.
 * Any call to Checkable methods will be passed to all child views which implement
 * this interface.
 *
 * @see <a href="http://www.marvinlabs.com/2010/10/29/custom-listview-ability-check-items/">http://www.marvinlabs.com/2010/10/29/custom-listview-ability-check-items/</a>
 * @author Matthias Düsterhöft
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {

    /**
     * Saves current checked state.
     */
    boolean isChecked;

    /**
     * List of all children implementing the {@link android.widget.Checkable} interface.
     */
    List<Checkable> checkableChildren;

    /**
     * @see android.widget.LinearLayout#LinearLayout(android.content.Context) LinearLayout
     */
    public CheckableLinearLayout(Context context) {
        super(context);
        init();
    }

    /**
     * @see android.widget.LinearLayout#LinearLayout(android.content.Context, android.util.AttributeSet) LinearLayout
     */
    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * @see android.widget.LinearLayout#LinearLayout(android.content.Context, android.util.AttributeSet, int) LinearLayout
     */
    public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        isChecked = false;
        checkableChildren = new ArrayList<Checkable>(1);
    }

    @Override
    public void setChecked(boolean checked) {
        isChecked = checked;
        for (Checkable checkable : checkableChildren) {
            checkable.setChecked(checked);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        isChecked = !isChecked;
        for (Checkable checkable : checkableChildren) {
            checkable.toggle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addCheckables(this);
    }

    /**
     * Adds all children which implement the {@link android.widget.Checkable} interface.
     * If a child is a group, it will be checked, too, by calling this method again
     * recursively.
     *
     * @param viewGroup View group
     */
    private void addCheckables(ViewGroup viewGroup) {
        int children = viewGroup.getChildCount();
        for (int child = 0; child < children; ++child) {
            View candidate = viewGroup.getChildAt(child);
            if (candidate instanceof Checkable) {
                checkableChildren.add((Checkable) candidate);
            }

            if (candidate instanceof ViewGroup) {
                addCheckables((ViewGroup) candidate);
            }
        }
    }
}
