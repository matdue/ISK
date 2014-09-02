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
