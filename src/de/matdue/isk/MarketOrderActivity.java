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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.database.OrderWatch;
import de.matdue.isk.eve.EveApi;

public class MarketOrderActivity extends IskActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		FragmentManager fm = getFragmentManager();
		if (fm.findFragmentById(android.R.id.content) == null) {
            CursorLoaderListFragment list = new CursorLoaderListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
            
        default:
    		return super.onOptionsItemSelected(item);
		}
	}
	
	public static class CursorLoaderListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, MarketOrderAdapter.MarketOrderListener {
		
		private String characterId;
		private IskDatabase iskDatabase;
		private MarketOrderAdapter adapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			setHasOptionsMenu(true);
			characterId = getActivity().getIntent().getStringExtra("characterID");
			iskDatabase = ((IskActivity)getActivity()).getDatabase();
			
			// Give some text to display if there is no data.
			setEmptyText(getResources().getText(R.string.wallet_no_data));
			
			adapter = new MarketOrderAdapter(getActivity(), 
					R.layout.market_order_entry, 
					null, 
					((IskActivity)getActivity()).getBitmapManager());
			setListAdapter(adapter);
			adapter.setMarketOrderListener(this);
			
			// Link to swipe helper
			ListViewSwipeHelper swipeHelper = new ListViewSwipeHelper(getActivity()) {
				@Override
				void onSwipe(View listView, View itemView, int position, Direction direction) {
					// On swipe, delete entry
					Long seqId = (Long) itemView.getTag(R.id.market_order_seq_id);
					new AsyncTask<Long, Void, Void>() {
						@Override
						protected Void doInBackground(Long... params) {
							iskDatabase.deleteOrderWatch(characterId, params[0]);
							return null;
						}

						protected void onPostExecute(Void result) {
							getLoaderManager().restartLoader(0, null, CursorLoaderListFragment.this);
						}
					}.execute(seqId);
				}
			};
			swipeHelper.setSwipableItemTagId(R.id.market_order_disposable);
			getListView().setOnTouchListener(swipeHelper);
			getListView().setOnScrollListener(swipeHelper);
			
			// Start out with a progress indicator.
            setListShown(false);
            
            // Prepare the loader. Either re-connect with an existing one, or start a new one.
            getLoaderManager().initLoader(0, null, this);
		}
		
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new SimpleCursorLoader(getActivity()) {
				@Override
				public Cursor loadInBackground() {
					return iskDatabase.queryOrderWatches(characterId);
				}
			};
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			// Swap the new cursor in.  (The framework will take care of closing the
            // old cursor once we return.)
            adapter.swapCursor(data);
            
            // The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }            
            
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			// This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
			adapter.swapCursor(null);
		}

		@Override
		public void onWatchChanged(final long seqId, final boolean isChecked) {
			// Save changed status in database
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					iskDatabase.setOrderWatchStatusWatch(characterId, seqId, isChecked);
					return null;
				}
				
				protected void onPostExecute(Void result) {
					getLoaderManager().restartLoader(0, null, CursorLoaderListFragment.this);
				}
			}.execute();
		}
		
	}
	
	static class MarketOrderAdapter extends ResourceCursorAdapter {
		
		public interface MarketOrderListener {
			public void onWatchChanged(long seqId, boolean isChecked);
		}
		
		private BitmapManager bitmapManager;
		private DateFormat dateFormatter;
		private NumberFormat numberFormatter;
		private NumberFormat integerFormatter;
		private MarketOrderListener marketOrderListener;
		
		public MarketOrderAdapter(Context context, int layout, Cursor c, BitmapManager bitmapManager) {
			super(context, layout, c);
			
			this.bitmapManager = bitmapManager;
			
			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			
			numberFormatter = NumberFormat.getInstance();
			numberFormatter.setMinimumFractionDigits(2);
			numberFormatter.setMaximumFractionDigits(2);
			
			integerFormatter = NumberFormat.getInstance();
			integerFormatter.setMinimumFractionDigits(0);
			integerFormatter.setMaximumFractionDigits(0);
		}
		
		public void setMarketOrderListener(MarketOrderListener listener) {
			marketOrderListener = listener;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// View holder pattern: http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
			// Cache elements instead of looking up using slow findViewById() 
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			if (viewHolder == null) {
				viewHolder = new ViewHolder();
				viewHolder.orderState = (TextView) view.findViewById(R.id.market_order_entry_orderstate);
				viewHolder.station = (TextView) view.findViewById(R.id.market_order_entry_station);
				viewHolder.item = (TextView) view.findViewById(R.id.market_order_entry_item);
				viewHolder.itemImage = (ImageView) view.findViewById(R.id.market_order_entry_item_image);
				viewHolder.price = (TextView) view.findViewById(R.id.market_order_entry_price);
				
				viewHolder.activeOrderGroup = view.findViewById(R.id.market_order_entry_incl_active);
				viewHolder.volume = (TextView) view.findViewById(R.id.market_order_entry_volume);
				viewHolder.expires = (TextView) view.findViewById(R.id.market_order_entry_expires);
				viewHolder.watch = (CheckBox) view.findViewById(R.id.market_order_entry_watch);
				viewHolder.watchDescription = (TextView) view.findViewById(R.id.market_order_entry_watch_desc);

				view.setTag(viewHolder);
			}
			Resources resources = context.getResources();
			
			String stationName = cursor.getString(6);
			if (stationName == null) {
				stationName = resources.getString(R.string.market_order_unknown_station, Integer.toString(cursor.getInt(5)));
			}
			viewHolder.station.setText(stationName);
			
			String itemName = cursor.getString(4);
			if (itemName == null) {
				itemName = resources.getString(R.string.market_order_unknown_item, Integer.toString(cursor.getInt(3)));
			}
			viewHolder.item.setText(itemName);
			
			String imageUrl = EveApi.getTypeUrl(Integer.toString(cursor.getInt(3)), 64);
			bitmapManager.setLoadingColor(Color.TRANSPARENT);
			bitmapManager.setImageBitmap(viewHolder.itemImage, imageUrl);
			
			BigDecimal price = new BigDecimal(cursor.getString(7));
			String sPrice = resources.getString(R.string.market_order_price_per_unit, numberFormatter.format(price) + " ISK");
			viewHolder.price.setText(sPrice);
			
			view.setTag(R.id.market_order_seq_id, cursor.getLong(15));
			
			int orderState = cursor.getInt(2);
			if (orderState == 0 && cursor.getLong(1) != 0) {
				// Active order
				int action = cursor.getInt(12);
				viewHolder.orderState.setText(action == 0 ? resources.getString(R.string.market_order_orderstate_sell) : resources.getString(R.string.market_order_orderstate_buy));
				
				int volEntered = cursor.getInt(8);
				int volRemaining = cursor.getInt(9);
				int fulfilled = cursor.getInt(10);
				String sVolume = resources.getString(R.string.market_order_fulfilled, fulfilled, integerFormatter.format(volEntered - volRemaining), integerFormatter.format(volEntered));
				viewHolder.volume.setText(sVolume);
				
				String sExpires = resources.getString(R.string.market_order_expires, dateFormatter.format(new Date(cursor.getLong(11))));
				viewHolder.expires.setText(sExpires);
				
				viewHolder.watch.setOnCheckedChangeListener(null);
				viewHolder.watch.setChecked((cursor.getInt(13) & OrderWatch.WATCH) != 0);
				viewHolder.watch.setTag(cursor.getLong(15));
				viewHolder.watch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (marketOrderListener != null) {
							marketOrderListener.onWatchChanged((Long) buttonView.getTag(), isChecked);
						}
					}
				});
				
				view.setTag(R.id.market_order_disposable, null);
				viewHolder.activeOrderGroup.setVisibility(View.VISIBLE);
			} else {
				// Inactive order
				int action = cursor.getInt(12);
				viewHolder.orderState.setText(action == 0 ? resources.getString(R.string.market_order_orderstate_sold) : resources.getString(R.string.market_order_orderstate_bought));
				
				view.setTag(R.id.market_order_disposable, Boolean.TRUE);
				viewHolder.activeOrderGroup.setVisibility(View.GONE);
			}
		}
		
		class ViewHolder {
			TextView orderState;
			TextView station;
			TextView item;
			ImageView itemImage;
			TextView price;

			View activeOrderGroup;
			TextView volume;
			TextView expires;
			CheckBox watch;
			TextView watchDescription;
		}
		
	}
	
}
