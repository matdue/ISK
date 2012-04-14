package de.matdue.isk.bitmap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

public class BitmapManager {
	
	// Memory cache
	private LruCache<String, Bitmap> memoryCache;
	
	// File cache
	private FileCache fileCache;
	
	// Cleanup cache at most every 24h
	private static final long CLEANUP_DELAY = 24l*60*60*1000;
	
	// Bitmap downloader
	private BitmapDownloader bitmapDownloader;
	
	// Show this image or color while loading
	private Integer loadingBitmap;
	private Integer loadingColor;
	
	private Context context;

	public BitmapManager(Context context, File cacheDir) {
		this.context = context;
		fileCache = new FileCache(cacheDir);
		bitmapDownloader = new BitmapDownloader(fileCache);
		
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
	
	public void shutdown() {
		bitmapDownloader.shutdown();
	}
	
	public void setLoadingBitmap(int resourceId) {
		loadingBitmap = resourceId;
		loadingColor = null;
	}
	
	public void setLoadingColor(int color) {
		loadingColor = color;
		loadingBitmap = null;
	}
	
	public void setImageBitmap(ImageView imageView, String imageUrl) {
		// Bitmap cached in memory?
		Bitmap cachedBitmap = memoryCache.get(imageUrl);
		if (cachedBitmap != null) {
			imageView.setImageBitmap(cachedBitmap);
			return;
		}

		// Download bitmap and set ImageView
		new DownloadTask(imageView).execute(imageUrl);
	}
	
	/**
	 * Cleanup file cache, if not done so in the last 24 hours
	 */
	private void fileCacheCleanup() {
		SharedPreferences preferences = context.getSharedPreferences("de.matdue.isk.bitmap.BitmapManager", Context.MODE_PRIVATE);
		long lastCleanup = preferences.getLong("lastCleanup", 0);
		long now = System.currentTimeMillis();
		if (lastCleanup + CLEANUP_DELAY < now) {
			// Last cleanup more than 24h ago => cleanup and save time
			fileCache.cleanup();
			preferences
				.edit()
				.putLong("lastCleanup", now)
				.commit();
		}
	}
	
	/**
	 * AsyncTask which will download bitmap from file cache or internet
	 * and store it in file cache, memory cache and ImageView.
	 */
	private class DownloadTask extends AsyncTask<String, Void, Bitmap> {
		
		private WeakReference<ImageView> imageViewReference;
		
		public DownloadTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
			
			// Show loading image
			Drawable downloadingDrawable;
			if (loadingBitmap != null) {
				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), loadingBitmap);
				downloadingDrawable = new DownloadingBitmapDrawable(this, context.getResources(), bitmap);
			} else if (loadingColor != null) {
				downloadingDrawable = new DownloadingColorDrawable(this, loadingColor);
			} else {
				downloadingDrawable = new DownloadingColorDrawable(this, Color.RED);
			}
			imageView.setImageDrawable(downloadingDrawable);
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			InputStream cachedImage = fileCache.getStream(params[0]);
			if (cachedImage != null) {
				Bitmap bitmap = BitmapFactory.decodeStream(cachedImage);
				memoryCache.put(params[0], bitmap);
				try {
					cachedImage.close();
				} catch (IOException e) {
					// Ignore errors
				}
				return bitmap;
			}
			
			Bitmap bitmap = bitmapDownloader.downloadBitmap(params[0]);
			memoryCache.put(params[0], bitmap);
			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			if (isCancelled()) {
				result = null;
			}

			if (imageViewReference != null) {
				ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					Drawable current = imageView.getDrawable();
					if (current instanceof IDownloadingDrawable) {
						IDownloadingDrawable currentDownloadingDrawable = (IDownloadingDrawable) current;
						if (currentDownloadingDrawable.getDownloadingTask() == this) {
							imageView.setImageBitmap(result);
						}
					}
				}
			}
		}
		
	}

}
