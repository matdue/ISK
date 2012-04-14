package de.matdue.isk.bitmap;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * Downloading drawable, which will show a bitmap.
 */
public class DownloadingBitmapDrawable extends BitmapDrawable implements IDownloadingDrawable {
	
	private final WeakReference<Object> bitmapDownloadTaskReference;
	
	public DownloadingBitmapDrawable(Object downloadTask, Resources res, Bitmap bitmap) {
		super(res, bitmap);
		bitmapDownloadTaskReference = new WeakReference<Object>(downloadTask);
	}
	
	@Override
	public Object getDownloadingTask() {
		return bitmapDownloadTaskReference.get();
	}

}
