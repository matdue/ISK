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
package de.matdue.isk.test;

import android.test.ActivityInstrumentationTestCase2;

import de.matdue.isk.MainActivity;
import de.matdue.isk.R;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity>
{
    private MainActivity _activity;

    public MainActivityTest()
    {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setActivityInitialTouchMode(false);
        _activity = getActivity();
    }

    public void testGetIndexedResourceString()
    {
        String knownResource = _activity.getIndexedResourceString(R.array.eve_reference_types, R.array.eve_reference_types_idx, 1, 0);
        assertEquals("Player Trading", knownResource);

        String unknownResource = _activity.getIndexedResourceString(R.array.eve_reference_types, R.array.eve_reference_types_idx, 999, 0);
        assertEquals("Undefined", unknownResource);

        String invalidResource = _activity.getIndexedResourceString(R.array.eve_reference_types, R.array.eve_reference_types_idx, -123, 0);
        assertEquals("Undefined", invalidResource);
    }
}
