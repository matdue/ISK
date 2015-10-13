/**
 * Copyright 2015 Matthias Düsterhöft
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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * {@link BitmapManager} will call methods of this interface while loading and when an image has
 * been downloaded.
 */
public interface BitmapReceiver {

    /**
     * BitmapManager needs something to detect if more than one request was issued for the same
     * item which will display the image. Provide any object as identifier, e.g. the
     * <code>ImageView</code> object.
     *
     * @return identifier object.
     */
    Object getDestination();

    /**
     * The image needs to be downloaded which may take some time. In the meantime,
     * <code>loadingImage</code> should be displayed. This method will not be called if the image
     * is already available in cache.
     *
     * @param loadingImage image to show while downloading. Optional (maybe null).
     */
    void onLoading(Drawable loadingImage);

    /**
     * The image is ready for display.
     *
     * @param bitmap bitmap for display.
     */
    void onReceive(Bitmap bitmap);

    /**
     * An error occurred while downloading.
     */
    void onError();

}