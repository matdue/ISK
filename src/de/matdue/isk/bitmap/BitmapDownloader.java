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

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class BitmapDownloader {

	private static int POOL_SIZE = 4;

	private ExecutorService pool;
	private AtomicInteger threadCounter;
	
	private FileCache cacheManager;

	public BitmapDownloader(FileCache cacheManager) {
		threadCounter = new AtomicInteger();
		this.cacheManager = cacheManager;

		pool = Executors.newFixedThreadPool(POOL_SIZE, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				thread.setName("BitmapDownloadManager thread #" + threadCounter.getAndIncrement());
				return thread;
			}

		});
	}

	public void shutdown() {
		pool.shutdown();
	}

	public Bitmap downloadBitmap(final String url) {
		Future<Bitmap> result = pool.submit(new Callable<Bitmap>() {

			@Override
			public Bitmap call() throws Exception {
				AndroidHttpClient httpClient = AndroidHttpClient.newInstance("de.matdue.isk");
				HttpGet request = new HttpGet(url);
				try {
					HttpResponse response = httpClient.execute(request);
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode != HttpStatus.SC_OK) {
						Log.e("BitmapDownloadManager", url + ": " + statusCode);
						return null;
					}

					HttpEntity entity = response.getEntity();
					if (entity != null) {
						InputStream inputStream = null;
						try {
							inputStream = entity.getContent();

							InputStream cachedStream = cacheManager.storeStream(url, inputStream, true);
							if (cachedStream != null) {
								// 'inputStream' cached successfully
								Bitmap bitmap = BitmapFactory.decodeStream(cachedStream);
								cachedStream.close();
								return bitmap;
							}
						} finally {
							if (inputStream != null) {
								inputStream.close();
							}
							entity.consumeContent();
						}
					}
				} catch (Exception e) {
					Log.e("BitmapDownloadManager", "Error downloading " + url, e);
					request.abort();
				} finally {
					httpClient.close();
				}

				return null;
			}

		});

		try {
			return result.get();
		} catch (Exception e) {
			Log.e("BitmapDownloadManager", "Error downloading " + url, e);
		}

		return null;
	}

}
