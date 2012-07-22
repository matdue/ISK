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
package de.matdue.isk;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * Helper class for handling swipe gestures on ListViews.
 * 
 * You need to mark items which are able to handle swipe gesture by on of these methods:
 * 
 * a) Implement SwipableItem
 * If a swipe has been detected, its method SwipableItem.onSwipe() will be called.
 * 
 * b) Mark item with a tag
 * Set a tag on the item view, e.g. item.setTag(R.id.xyz, true).
 * If a swipe has been detected, ListViewSwipeHelper.onSwipe() will be called.
 * Don't forget to override this method, and set the tag id
 * via setSwipableItemTagId().
 * 
 * @author Matthias Düsterhöft
 */
public class ListViewSwipeHelper implements View.OnTouchListener, AbsListView.OnScrollListener {

	/**
	 * Swipe direction
	 */
	public enum Direction {
		RIGHT,
		LEFT
	}
	
	/**
	 * Implement this interface if the item is allowed for being swiped.
	 * If you do not like implementing this interface, mark an item
	 * for being swipable by setting a tag with a non-null value, e.g. item.setTag(R.id.xyz, true)
	 * and provide the id by calling setSwipableItemTagId().
	 */
	public interface SwipableItem {
		/**
		 * If a swipe has been detected, this method is called.
		 * 
		 * @param listView ListView
		 * @param position Position
		 * @param direction Swipe direction
		 */
		public void onSwipe(View listView, int position, Direction direction);
	}
	
	/**
	 * If a swipe has been detected, this method is called,
	 * UNLESS the item view implements SwipableItem.
	 * This implementation does nothing.
	 * 
	 * @param listView ListView
	 * @param itemView Item's view
	 * @param position Position
	 * @param direction Swipe direction
	 */
	void onSwipe(View listView, View itemView, int position, Direction direction) {
	}
	
	/**
	 * ID of tag an item must have (if it does not implement SwipableItem)
	 */
	protected int swipableItemTagId;
	
	/**
	 * Current mode
	 */
	protected enum SwipeMode {
		NONE,
		MOVING,
		IGNORING
	};
	protected SwipeMode currentSwipeMode = SwipeMode.NONE;
	
	/**
	 * Remember if ListView is scrolling at the moment
	 */
	protected boolean isScrolling;
	
	/**
	 * Number of pixels swiped until we switch to swipe mode
	 */
	protected int touchSlop;
	
	/**
	 * If item has been swiped more than 40% to the left or to the right,
	 * fire swipe event and make it (nearly) transparent
	 */
	protected float swipeThreshold = 0.4f;
	
	/**
	 * X coordinate swipe started at
	 */
	protected float swipeInitialX;
	
	/**
	 * View of the item on which the swipe gesture happens
	 */
	protected View itemView;
	
	/**
	 * Position of the item on which the swipe gesture happens
	 */
	protected int itemViewPosition;
	
	/**
	 * Original X coordinate of item view
	 */
	protected float itemViewOriginalX;
	
	/**
	 * Original alpha value of item view
	 */
	protected float itemViewOriginalAlpha;
	
	
	public ListViewSwipeHelper(Context context) {
		// Get value for 'touchSlop' from Android system
		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		touchSlop = viewConfiguration.getScaledTouchSlop();
	}
	
	public void setSwipableItemTagId(int tagId) {
		this.swipableItemTagId = tagId;
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// Intentionally no code
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		isScrolling = scrollState == SCROLL_STATE_TOUCH_SCROLL;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// Possible start of swipe gesture
			swipeInitialX = event.getX();
			currentSwipeMode = SwipeMode.NONE;
			break;
			
		case MotionEvent.ACTION_MOVE:
			if (currentSwipeMode == SwipeMode.MOVING) {
				// Move item left or right
				float swiped = event.getX() - swipeInitialX;
				itemView.setX(itemViewOriginalX + swiped);
				
				// The more the item has been swiped, the less opaque it will become,
				// but do not fall below 0.05
				float opaqueWidth = itemView.getWidth() * swipeThreshold;
				float transparency = Math.min(Math.abs(swiped) / opaqueWidth, 1.0f);
				itemView.setAlpha(Math.max(itemViewOriginalAlpha - transparency, 0.05f));
				
				return true; // Event has been consumed
			}
			else if (currentSwipeMode == SwipeMode.NONE && !isScrolling && Math.abs(event.getX() - swipeInitialX) > touchSlop) {
				// Gesture is a swipe to the right or left, and ListView is not scrolling
				// => Start watching this gesture
				
				// Get the item view on which the swipe gesture happens
				itemView = null;
				ListView listView = (ListView)v;
				itemViewPosition = listView.pointToPosition((int)event.getX(), (int)event.getY());
				int firstPosition = listView.getFirstVisiblePosition() - listView.getHeaderViewsCount();
				int wantedChild = itemViewPosition - firstPosition;
				if (wantedChild >= 0 && wantedChild < listView.getChildCount()) {
					itemView = listView.getChildAt(wantedChild);
					
					// Does this item support swiping?
					if (itemView instanceof SwipableItem || itemView.getTag(swipableItemTagId) != null) {
						// Item does support swiping
						itemViewOriginalX = itemView.getX();
						itemViewOriginalAlpha = itemView.getAlpha();
						currentSwipeMode = SwipeMode.MOVING;
						
						// Modify event and let ListView see an ACTION_CANCEL event
						// ListView will reset its state then
						event.setAction(MotionEvent.ACTION_CANCEL);
						return false;
					} else {
						// Item does not support swiping
						currentSwipeMode = SwipeMode.IGNORING;
					}
				}
			}
			break;
			
		case MotionEvent.ACTION_UP:
			if (currentSwipeMode == SwipeMode.MOVING && (Math.abs(event.getX() - swipeInitialX) > itemView.getWidth() * swipeThreshold)) {
				// Gesture has been finished and item has been moved far enough
				// Fire swipe event
				if (itemView instanceof SwipableItem) {
					((SwipableItem)itemView).onSwipe(v, itemViewPosition, event.getX() > swipeInitialX ? Direction.RIGHT : Direction.LEFT);
				} else {
					onSwipe(v, itemView, itemViewPosition, event.getX() > swipeInitialX ? Direction.RIGHT : Direction.LEFT);
				}
				
				// Fade out smoothly
				ValueAnimator collapseAnimator = ValueAnimator.ofInt(itemView.getHeight(), 1);
				collapseAnimator.addUpdateListener(new HeightSetter(itemView));
				
				float destX = itemView.getX() >= 0 ? itemView.getWidth() : -itemView.getWidth();
				ObjectAnimator fadeOutAnimator = ObjectAnimator.ofPropertyValuesHolder(itemView, 
						PropertyValuesHolder.ofFloat("x", itemView.getX(), destX),
						PropertyValuesHolder.ofFloat("alpha", itemView.getAlpha(), 0.0f));
				
				AnimatorSet animator = new AnimatorSet();
				animator.playTogether(collapseAnimator, fadeOutAnimator);
				animator.setDuration(150);
				animator.start();

				itemView = null;
				currentSwipeMode = SwipeMode.NONE;
				return true; // Event has been consumed
			}
			// fall through
			
		case MotionEvent.ACTION_CANCEL:
			if (currentSwipeMode == SwipeMode.MOVING) {
				// Gesture has been stopped, or user did not move item far enough
				// Restore item position and transparency smoothly
				ViewPropertyAnimator animator = itemView.animate();
				animator.x(itemViewOriginalX);
				animator.alpha(itemViewOriginalAlpha);
				animator.setDuration(150);
				
				itemView = null;
				currentSwipeMode = SwipeMode.NONE;
				return true; // Event has been consumed
			} else if (currentSwipeMode == SwipeMode.IGNORING) {
				itemView = null;
				currentSwipeMode = SwipeMode.NONE;
			}
			break;
		}

		// Event not processed
		return false;
	}

}
