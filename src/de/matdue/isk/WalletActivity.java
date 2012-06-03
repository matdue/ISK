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

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.eve.EveApi;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

public class WalletActivity extends IskActivity {
	
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
	
	public static class CursorLoaderListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnQueryTextListener {
		
		private WalletAdapter adapter;
		private String searchFilter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			setHasOptionsMenu(true);
			
			// Give some text to display if there is no data.
			setEmptyText(getResources().getText(R.string.wallet_no_data));
			
			adapter = new WalletAdapter(getActivity(), 
					R.layout.wallet_entry, 
					null,
					((IskActivity)getActivity()).getBitmapManager());
			setListAdapter(adapter);
			
			// Start out with a progress indicator.
            setListShown(false);
            
            // Prepare the loader. Either re-connect with an existing one, or start a new one.
            getLoaderManager().initLoader(0, null, this);
		}
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.wallet_options, menu);
			((SearchView)menu.findItem(R.id.search).getActionView()).setOnQueryTextListener(this);
			
			super.onCreateOptionsMenu(menu, inflater);
		}
		
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new SimpleCursorLoader(getActivity()) {
				@Override
				public Cursor loadInBackground() {
					String characterId = getActivity().getIntent().getStringExtra("characterID");
					return ((IskActivity)getActivity()).getDatabase().getEveWallet(characterId, searchFilter);
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
		public boolean onQueryTextChange(String newText) {
			searchFilter = newText;
			getLoaderManager().restartLoader(0, null, this);
			return true;
		}

		@Override
		public boolean onQueryTextSubmit(String query) {
			// Do nothing, everything is handled by onQueryTextChange()
			return true;
		}
		
	}

	static class WalletAdapter extends ResourceCursorAdapter {
		
		private BitmapManager bitmapManager;
		private DateFormat dateFormatter;
		private NumberFormat numberFormatter;
		private NumberFormat integerFormatter;
		private int positiveNumber;
		private int negativeNumber;
		
		public WalletAdapter(Context context, int layout, Cursor c, BitmapManager bitmapManager) {
			super(context, layout, c);
			
			this.bitmapManager = bitmapManager;
			
			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			
			numberFormatter = NumberFormat.getInstance();
			numberFormatter.setMinimumFractionDigits(2);
			numberFormatter.setMaximumFractionDigits(2);
			
			integerFormatter = NumberFormat.getInstance();
			integerFormatter.setMinimumFractionDigits(0);
			integerFormatter.setMaximumFractionDigits(0);
			
			positiveNumber = context.getResources().getColor(R.color.wallet_entry_number_positive);
			negativeNumber = context.getResources().getColor(R.color.wallet_entry_number_negative);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// View holder pattern: http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
			// Cache elements instead of looking up using slow findViewById() 
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			if (viewHolder == null) {
				viewHolder = new ViewHolder();
				viewHolder.defaultInclude = view.findViewById(R.id.wallet_entry_incl_default);
				viewHolder.marketInclude = view.findViewById(R.id.wallet_entry_incl_market);
				viewHolder.date = (TextView) view.findViewById(R.id.wallet_entry_date);
				
				viewHolder.defaultType = (TextView) view.findViewById(R.id.wallet_entry_default_type);
				viewHolder.defaultAmount = (TextView) view.findViewById(R.id.wallet_entry_default_amount);
				viewHolder.defaultTaxStatic = view.findViewById(R.id.wallet_entry_default_tax_static);
				viewHolder.defaultTax = (TextView) view.findViewById(R.id.wallet_entry_default_tax);

				viewHolder.marketAction = (TextView) view.findViewById(R.id.wallet_entry_market_action);
				viewHolder.marketPartner = (TextView) view.findViewById(R.id.wallet_entry_market_partner);
				viewHolder.marketStation = (TextView) view.findViewById(R.id.wallet_entry_market_station);
				viewHolder.marketItem = (TextView) view.findViewById(R.id.wallet_entry_market_item);
				viewHolder.marketItemImage = (ImageView) view.findViewById(R.id.wallet_entry_market_item_image);
				viewHolder.marketQuantity = (TextView) view.findViewById(R.id.wallet_entry_market_quantity);
				viewHolder.marketSinglePrice = (TextView) view.findViewById(R.id.wallet_entry_market_single_price);
				viewHolder.marketTotalPrice = (TextView) view.findViewById(R.id.wallet_entry_market_total_price);

				view.setTag(viewHolder);
			}
			
			// Date and time
			viewHolder.date.setText(dateFormatter.format(new Date(cursor.getLong(1))));

			viewHolder.defaultInclude.setVisibility(View.GONE);
			viewHolder.marketInclude.setVisibility(View.GONE);
			switch (cursor.getInt(2)) {
			case 2:
				// Market transaction
			case 42:
				// Market escrow
				
				// Distinguish between initial market escrow (when you place the order)
				// and fullfillment of order.
				if (cursor.isNull(10)) {
					// Initial market escrow
					formatDefault(viewHolder, context, cursor);
				} else {
					formatMarketTransaction(viewHolder, context, cursor, true);
				}
				break;
				
			case -42:
				// Transaction, already payed by market escrow
				formatMarketTransaction(viewHolder, context, cursor, false);
				break;
				
			default:
				formatDefault(viewHolder, context, cursor);
				break;
			}
		}
		
		private void formatMarketTransaction(ViewHolder viewHolder, Context context, Cursor cursor, boolean hasWalletEntry) {
			viewHolder.marketInclude.setVisibility(View.VISIBLE);
			
			String transactionType = cursor.getString(13);
			String transactionFor = cursor.getString(14);
			if ("buy".equals(transactionType)) {
				if ("personal".equals(transactionFor)) {
					viewHolder.marketAction.setText(R.string.wallet_personal_buy);
				} else {
					viewHolder.marketAction.setText(R.string.wallet_corp_buy);
				}
			} else {
				if ("personal".equals(transactionFor)) {
					viewHolder.marketAction.setText(R.string.wallet_personal_sell);
				} else {
					viewHolder.marketAction.setText(R.string.wallet_corp_sell);
				}
			}
			
			viewHolder.marketPartner.setText(cursor.getString(11));
			viewHolder.marketStation.setText(cursor.getString(12));
			viewHolder.marketItem.setText(cursor.getString(9));
			String imageUrl = EveApi.getTypeUrl(cursor.getString(15), 64);
			bitmapManager.setLoadingColor(Color.TRANSPARENT);
			bitmapManager.setImageBitmap(viewHolder.marketItemImage, imageUrl);
			
			int quantity = cursor.getInt(8);
			viewHolder.marketQuantity.setText(integerFormatter.format(quantity));
			
			BigDecimal singlePrice = new BigDecimal(cursor.getString(10));
			String sSinglePrice = numberFormatter.format(singlePrice) + " ISK";
			viewHolder.marketSinglePrice.setText(sSinglePrice);
			
			BigDecimal totalPrice = new BigDecimal(cursor.getString(6));
			String sTotalPrice = numberFormatter.format(totalPrice) + " ISK";
			if (!hasWalletEntry) {
				sTotalPrice = "(" + sTotalPrice + ")";
			}
			viewHolder.marketTotalPrice.setText(sTotalPrice);
			viewHolder.marketTotalPrice.setTextColor(totalPrice.compareTo(BigDecimal.ZERO) < 0 ? negativeNumber : positiveNumber);
		}
		
		private void formatDefault(ViewHolder viewHolder, Context context, Cursor cursor) {
			viewHolder.defaultInclude.setVisibility(View.VISIBLE);
			
			int refType = cursor.getInt(2);
			viewHolder.defaultType.setText(getIndexedResourceString(context, R.array.eve_reference_types, R.array.eve_reference_types_idx, refType, 0));
			
			BigDecimal amount = new BigDecimal(cursor.getString(6));
			BigDecimal tax = new BigDecimal(cursor.getString(7));
			amount = amount.add(tax);  // Amount has the same value now as in EVE Online
			tax = tax.negate();
			String sAmount = numberFormatter.format(amount) + " ISK";
			viewHolder.defaultAmount.setText(sAmount);
			viewHolder.defaultAmount.setTextColor(amount.compareTo(BigDecimal.ZERO) < 0 ? negativeNumber : positiveNumber);
			
			int taxComparedToZero = tax.compareTo(BigDecimal.ZERO);
			if (taxComparedToZero == 0) {
				viewHolder.defaultTaxStatic.setVisibility(View.GONE);
				viewHolder.defaultTax.setVisibility(View.GONE);
			} else {
				viewHolder.defaultTaxStatic.setVisibility(View.VISIBLE);
				viewHolder.defaultTax.setVisibility(View.VISIBLE);
				String sTax = numberFormatter.format(tax) + " ISK";
				viewHolder.defaultTax.setText(sTax);
				viewHolder.defaultTax.setTextColor(taxComparedToZero < 0 ? negativeNumber : positiveNumber);
			}
		}
		
		class ViewHolder {
			View defaultInclude;
			View marketInclude;
			TextView date;
			
			TextView defaultType;
			TextView defaultAmount;
			View defaultTaxStatic;
			TextView defaultTax;
			
			TextView marketAction;
			TextView marketPartner;
			TextView marketStation;
			TextView marketItem;
			ImageView marketItemImage;
			TextView marketQuantity;
			TextView marketSinglePrice;
			TextView marketTotalPrice;
		}
	}

}
