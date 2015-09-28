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
package de.matdue.isk.bitmap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

public class BitmapManager {
	
	// Memory cache
	private LruCache<String, Bitmap> memoryCache;
	
	// File cache
	private FileCache fileCache;
	
	// Cleanup cache at most every 24h
	private static final long CLEANUP_DELAY = 24l*60*60*1000;

	// Currently running downloads
	private final HashMap<String, List<BitmapReceiver>> todoList = new HashMap<String, List<BitmapReceiver>>();
	
	private Context context;

	public BitmapManager(Context context, File cacheDir) {
		this.context = context;
		fileCache = new FileCache(cacheDir);

		// Use 1/8th of the available memory for memory cache
		int memClass = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		int cacheSize = memClass * 1024 * 1024 / 8;
		memoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// The cache size will be measured in bytes rather than number of items
				return value.getByteCount();
			}
		};
		
		fileCacheCleanup();
	}

	public void clearMemoryCache() {
		memoryCache.evictAll();
	}

	/**
	 * Cleanup file cache, if not done so in the last 24 hours.
	 * This method starts a background task using {@link AsyncTask} and returns immediately.
	 */
	private void fileCacheCleanup() {
		// Disk I/O => do cleanup in background thread
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SharedPreferences preferences = context.getSharedPreferences("de.matdue.isk.bitmap.BitmapManager", Context.MODE_PRIVATE);
				long lastCleanup = preferences.getLong("lastCleanup", 0);
				long now = System.currentTimeMillis();
				if (lastCleanup + CLEANUP_DELAY < now) {
					// Last cleanup more than 24h ago => cleanup and save time
					fileCache.cleanup();
					preferences
							.edit()
							.putLong("lastCleanup", now)
							.apply();
				}

				return null;
			}
		}.execute();
	}

	/**
	 * Load an image asynchronously.
	 * This method returns immediately. If the image is in memory cache,
	 * all work is done. Otherwise, a background thread will load the image.
	 *
	 * All threads are running concurrently. There is no guarantee that requests are handled
	 * in sequence. If you set multiple URLs for the same image, the final URL may not correspond
	 * to the last call of this method.
	 *
	 * @param bitmapReceiver FIXME: ImageView which will receive the bitmap
	 * @param imageUrl URL of bitmap
	 * @param loadingBitmap Show this resource bitmap while loading
	 * @param loadingColor Show this color while loading
	 */
	public void setImageBitmap(BitmapReceiver bitmapReceiver, String imageUrl, Integer loadingBitmap, Integer loadingColor) {
		// Bitmap cached in memory?
		Bitmap cachedBitmap = memoryCache.get(imageUrl);
		if (cachedBitmap != null) {
			Log.d("BitmapManager", "In memory cache: " + imageUrl);
			bitmapReceiver.onReceive(cachedBitmap);
			return;
		}

		// Show loading image
		Drawable downloadingDrawable;
		if (loadingBitmap != null) {
			downloadingDrawable = context.getResources().getDrawable(loadingBitmap);
		} else if (loadingColor != null) {
			downloadingDrawable = new ColorDrawable(loadingColor);
		} else {
			downloadingDrawable = new ColorDrawable(Color.TRANSPARENT);
		}
		bitmapReceiver.onLoading(downloadingDrawable);

		// Register URL for download. There is a list of images for each URL.
		// As soon as the images has been downloaded, all registered images will receive the
		// downladed bitmap. This prevents multiple downloads of the same URL.
		boolean urlIsDownloading = false;
		synchronized (todoList) {
			// Make sure there is only one BitmapReceiver for a particular destination.
			// This ensures that only the last of multiple requests for the same destination
			// will deliver the bitmap.
			removeDestinationFromTodoList(bitmapReceiver.getDestination());

			// Add URL, unless it is already being downloaded
			List<BitmapReceiver> bitmapReceivers = todoList.get(imageUrl);
			if (bitmapReceivers == null) {
				bitmapReceivers = new ArrayList<BitmapReceiver>();
				todoList.put(imageUrl, bitmapReceivers);
				Log.d("BitmapManager", "Initiate loading: " + imageUrl);
			} else {
				urlIsDownloading = true;
				Log.d("BitmapManager", "Already loading: " + imageUrl);
			}

			// Add BitmapReceiver to list of receivers
			bitmapReceivers.add(bitmapReceiver);
		}

		// Download bitmap and set ImageView
		if (!urlIsDownloading) {
			new DownloadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageUrl);
		}
	}

	/**
	 * Remove any entry from {@link #todoList} with destination <code>destination</code>.
	 * Do nothing if <code>destination</code> is <code>null</code>.
	 *
	 * @param destination Destination element
	 */
	private void removeDestinationFromTodoList(Object destination) {
		if (destination != null) {
			for (Iterator<Map.Entry<String, List<BitmapReceiver>>> todoListIterator = todoList.entrySet().iterator(); todoListIterator.hasNext(); ) {
				Map.Entry<String, List<BitmapReceiver>> entry = todoListIterator.next();
				List<BitmapReceiver> receivers = entry.getValue();
				for (Iterator<BitmapReceiver> receiverIterator = receivers.iterator(); receiverIterator.hasNext(); ) {
					BitmapReceiver receiver = receiverIterator.next();
					if (receiver.getDestination() == destination) {
						receiverIterator.remove();
					}
				}

				if (receivers.isEmpty()) {
					todoListIterator.remove();
				}
			}
		}
	}

	public void setImageBitmap(ImageView imageView, String imageUrl, Integer loadingBitmap, Integer loadingColor) {
		setImageBitmap(new BitmapReceiverForImageView(imageView), imageUrl, loadingBitmap, loadingColor);
	}
	
	/**
	 * Load an image synchronously.
	 * This method blocks until the image has been loaded.
	 * 
	 * @param imageUrl URL of bitmap
	 * @return Loaded bitmap, or <code>null</code>
	 */
	public Bitmap getImage(String imageUrl) {
		// Bitmap cached in memory?
		Bitmap cachedBitmap = memoryCache.get(imageUrl);
		if (cachedBitmap != null) {
			return cachedBitmap;
		}
		
		// In file cache?
		InputStream cachedImage = fileCache.getStream(imageUrl);
		if (cachedImage != null) {
			Bitmap bitmap = BitmapFactory.decodeStream(cachedImage);
			if (bitmap != null) {
				// Cache in memory
				memoryCache.put(imageUrl, bitmap);
			}
			try {
				cachedImage.close();
			} catch (IOException e) {
				// Ignore errors
			}
			return bitmap;
		}

		// Download and cache in memory
		Bitmap bitmap = downloadBitmap(imageUrl);
		if (bitmap != null) {
			memoryCache.put(imageUrl, bitmap);
		}
		return bitmap;
	}

	/**
	 * Download an image URL.
	 *
	 * @param url URL
	 * @return Image, or <code>null</code> if download failed.
	 */
	private Bitmap downloadBitmap(String url) {
		try {
			Log.d("BitmapManager", "Downloading: " + url);
			URL downloadUrl = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
			connection.setRequestProperty("User-Agent", "blubb");

			int statusCode = connection.getResponseCode();
			if (statusCode != HttpURLConnection.HTTP_OK) {
				Log.e("BitmapManager", url + ": " + statusCode);
				return null;
			}

			InputStream inputStream = null;
			try {
				inputStream = connection.getInputStream();
				InputStream cachedStream = fileCache.storeStream(url, inputStream, true);
				if (cachedStream != null) {
					// 'inputStream' cached successfully
					Bitmap bitmap = BitmapFactory.decodeStream(cachedStream);
					cachedStream.close();

					memoryCache.put(url, bitmap);

					Log.d("BitmapManager", "Download finished: " + url);
					return bitmap;
				}
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		} catch (Exception e) {
			Log.e("BitmapManager", "Error downloading " + url, e);
		}

		return null;
	}
	
	/**
	 * AsyncTask which will download bitmap from file cache or internet
	 * and store it in file cache, memory cache and ImageView.
	 */
	private class DownloadTask extends AsyncTask<String, Void, Bitmap> {

		private String url;
		
		@Override
		protected Bitmap doInBackground(String... params) {
			url = params[0];
			InputStream cachedImage = fileCache.getStream(url);
			if (cachedImage != null) {
				Bitmap bitmap = BitmapFactory.decodeStream(cachedImage);
				if (bitmap != null) {
					memoryCache.put(url, bitmap);
				}
				try {
					cachedImage.close();
				} catch (IOException e) {
					// Ignore errors
				}
				Log.d("BitmapManager", "Got from file cache: " + url);
				return bitmap;
			}

			return downloadBitmap(url);
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			if (isCancelled()) {
				result = null;
			}

			// Get list of images which should receive the fresh loaded bitmap
			List<BitmapReceiver> bitmapReceivers;
			synchronized (todoList) {
				bitmapReceivers = todoList.remove(url);
			}

			// Set bitmap
			Log.d("BitmapManager", "Setting " + (bitmapReceivers != null ? bitmapReceivers.size() : 0) + " image(s) for: " + url);
			if (bitmapReceivers != null) {
				for (BitmapReceiver bitmapReceiver : bitmapReceivers) {
					if (result != null) {
						bitmapReceiver.onReceive(result);
					} else {
						bitmapReceiver.onError();
					}
				}
			}
		}
		
	}

}
