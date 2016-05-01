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
 * Task to download station data from EVE Online CREST API.
 */
public class DownloadStationTask extends DownloadTask {

    /**
     * Station ID
     */
    private String _stationId;

    /**
     * Result will be stored in this map.
     */
    private final Map<String, Station> _result;

    /**
     * Creates a task to download details of a specific station.
     *
     * @param stationId the station ID.
     * @param result result will be stored in this map with the ID as key. If there are several
     *               download tasks running concurrently, make sure this maps is able to handle
     *               concurrent accesses, e.g. {@link java.util.concurrent.ConcurrentHashMap}.
     */
    public DownloadStationTask(String stationId, Map<String, Station> result) {
        super("https://crest-tq.eveonline.com/stations/" + stationId + "/");
        _stationId = stationId;
        _result = result;
    }

    /**
     * Returns the station ID. This task will download details for this station.
     *
     * @return the station ID.
     */
    public String getStationId() {
        return _stationId;
    }

    @Override
    protected void process(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            Gson gson = new Gson();
            Station station = gson.fromJson(reader, Station.class);
            if (station != null) {
                station.id = _stationId;
                _result.put(_stationId, station);
            }
        } finally {
            reader.close();
        }
    }

    @Override
    protected void processFailure(int responseCode, String responseMessage, InputStream errorStream) throws IOException {
        Log.w("DownloadTypeTask", "Error retrieving details of station " + _stationId + ": " + responseCode + " " + responseMessage);
    }

    @Override
    protected void handleException(IOException e) {
        Log.w("DownloadTypeTask", "Error retrieving details of station " + _stationId, e);
    }
}
