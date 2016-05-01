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
package de.matdue.isk.download;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.matdue.isk.BuildConfig;

/**
 * Base class for a task which downloads a resource. Use {@link DownloadPool} for running
 * multiple tasks concurrently.
 */
public abstract class DownloadTask implements Runnable {

    /**
     * URL to download
     */
    private String _url;

    /**
     * Creates a new download task.
     *
     * @param url the URL to download.
     */
    public DownloadTask(String url) {
        _url = url;
    }

    /**
     * Returns the URL to download.
     *
     * @return the URL to download.
     */
    public String GetUrl() {
        return _url;
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        try {
            Log.d("DownloadTask", "Downloading " + _url);
            URL requestURL = new URL(_url);
            connection = (HttpURLConnection) requestURL.openConnection();
            connection.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME);

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                Log.d("DownloadTask", "Downloading " + _url + " failed: " + statusCode);
                processFailure(statusCode, connection.getResponseMessage(), connection.getErrorStream());
            } else {
                Log.d("DownloadTask", "Downloading " + _url + " finished");
                process(connection.getInputStream());
            }
        } catch (IOException e) {
            Log.d("DownloadTask", "Downloading " + _url + " failed", e);
            handleException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Called when download is successful. You must close the given stream.
     *
     * @param inputStream stream with data. Close it after processing.
     * @throws IOException if there is an IO error during the retrieval.
     */
    protected abstract void process(InputStream inputStream) throws IOException;

    /**
     * Called when the server did not respond with an <code>200 OK</code>.
     *
     * @param responseCode the response code, e.g. <code>404</code>.
     * @param responseMessage the response message, e.g. <code>NOT FOUND</code>
     * @param errorStream the error stream with data the server sends back.
     * @throws IOException if there is an IO error during the retrieval.
     */
    protected abstract void processFailure(int responseCode, String responseMessage, InputStream errorStream) throws IOException;

    /**
     * Called when an error occurred, e.g. a network failure.
     * @param e the exception.
     */
    protected abstract void handleException(IOException e);
}
