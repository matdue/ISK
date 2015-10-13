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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class BitmapReceiverForImageView implements BitmapReceiver {

    private WeakReference<ImageView> imageViewReference;
    private boolean displayLoadingImage;

    public BitmapReceiverForImageView(ImageView imageView) {
        // Wrap image with a WeakReference. A long lasting download should not prevent the
        // garbage collector from removing a meanwhile unused ImageView and its context.
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    @Override
    public Object getDestination() {
        return imageViewReference.get();
    }

    @Override
    public void onLoading(Drawable loadingImage) {
        ImageView imageView = imageViewReference.get();
        if (imageView != null) {
            imageView.setImageDrawable(loadingImage);
            displayLoadingImage = true;
        }
    }

    @Override
    public void onReceive(Bitmap bitmap) {
        ImageView imageView = imageViewReference.get();
        if (imageView == null) {
            // ImageView has already been garbage collected and is not visible any more.
            return;
        }

        if (!displayLoadingImage) {
            // Show image at once if there was no loading time
            imageView.setImageBitmap(bitmap);
            return;
        }

        Drawable currentDrawable = imageView.getDrawable();
        if (currentDrawable == null) {
            // No previous or loading image is displayed => set new bitmap without transition
            imageView.setImageBitmap(bitmap);
            return;
        }

        // If current drawable is a TransitionDrawable,
        // transition from its final drawable
        if (currentDrawable instanceof TransitionDrawable) {
            currentDrawable = ((TransitionDrawable) currentDrawable).getDrawable(1);
        }

        // Show new bitmap with crossfading transition
        Drawable newDrawable = new BitmapDrawable(imageView.getResources(), bitmap);
        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{currentDrawable, newDrawable});
        transitionDrawable.setCrossFadeEnabled(true);
        imageView.setImageDrawable(transitionDrawable);

        int animationDuration = imageView.getResources().getInteger(android.R.integer.config_shortAnimTime);
        transitionDrawable.startTransition(animationDuration);
    }

    @Override
    public void onError() {
    }

}