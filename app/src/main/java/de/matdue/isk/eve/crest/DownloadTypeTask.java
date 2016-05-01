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
 * Task to download type data from EVE Online CREST API.
 */
public class DownloadTypeTask extends DownloadTask {

    /**
     * Type ID
     */
    private String _typeId;

    /**
     * Result will be stored in this map.
     */
    private final Map<String, Type> _result;

    /**
     * Creates a task to download details of a specific type.
     *
     * @param typeId the type ID.
     * @param result result will be stored in this map with the ID as key. If there are several
     *               download tasks running concurrently, make sure this maps is able to handle
     *               concurrent accesses, e.g. {@link java.util.concurrent.ConcurrentHashMap}.
     */
    public DownloadTypeTask(String typeId, Map<String, Type> result) {
        super("https://crest-tq.eveonline.com/types/" + typeId + "/");
        _typeId = typeId;
        _result = result;
    }

    /**
     * Returns the type ID. This task will download details for this type.
     *
     * @return the type ID.
     */
    public String getTypeId() {
        return _typeId;
    }

    @Override
    protected void process(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            Gson gson = new Gson();
            Type type = gson.fromJson(reader, Type.class);
            if (type != null) {
                _result.put(_typeId, type);
            }
        } finally {
            reader.close();
        }
    }

    @Override
    protected void processFailure(int responseCode, String responseMessage, InputStream errorStream) {
        Log.w("DownloadTypeTask", "Error retrieving details of type " + _typeId + ": " + responseCode + " " + responseMessage);
    }

    @Override
    protected void handleException(IOException e) {
        Log.w("DownloadTypeTask", "Error retrieving details of type " + _typeId, e);
    }
}
