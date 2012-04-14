package de.matdue.isk.bitmap;

import java.lang.ref.WeakReference;

import android.graphics.drawable.ColorDrawable;

/**
 * Downloading drawable, which will show just a color.
 */
public class DownloadingColorDrawable extends ColorDrawable implements IDownloadingDrawable {
	
	private final WeakReference<Object> bitmapDownloadTaskReference;
	
	public DownloadingColorDrawable(Object downloadTask, int color) {
		super(color);
		bitmapDownloadTaskReference = new WeakReference<Object>(downloadTask);
	}

	@Override
	public Object getDownloadingTask() {
		return bitmapDownloadTaskReference.get();
	}

}
