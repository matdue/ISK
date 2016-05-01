/**
 * Copyright 2016 Matthias Düsterhöft
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
package de.matdue.isk.eve.crest;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

import de.matdue.isk.download.DownloadTask;

/**
 * Task to download all facility data from EVE Online CREST API.
 */
public class DownloadFacilitiesTask extends DownloadTask {

    /**
     * Result will be stored in this map.
     */
    private final Map<String, Facility> _result;

    /**
     * Creates a task to download all facilities.
     *
     * @param result result will be stored in this map with the ID as key.
     */
    public DownloadFacilitiesTask(Map<String, Facility> result) {
        super("https://crest-tq.eveonline.com/industry/facilities/");
        _result = result;
    }

    @Override
    protected void process(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            Gson gson = new Gson();
            IndustryFacilities industryFacilities = gson.fromJson(reader, IndustryFacilities.class);
            if (industryFacilities != null) {
                for (Facility facility : industryFacilities.items) {
                    _result.put(facility.facilityID, facility);
                }
            }
        } finally {
            reader.close();
        }
    }

    @Override
    protected void processFailure(int responseCode, String responseMessage, InputStream errorStream) {
        Log.w("DownloadFacilityTask", "Error retrieving industry facilities: " + responseCode + " " + responseMessage);
    }

    @Override
    protected void handleException(IOException e) {
        Log.w("DownloadFacilityTask", "Error retrieving industry facilities", e);
    }
}
