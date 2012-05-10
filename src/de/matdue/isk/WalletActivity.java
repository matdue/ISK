package de.matdue.isk;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.eve.EveApi;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class WalletActivity extends ListActivity {
	
	private BitmapManager bitmapManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.wallet);
		
		IskApplication iskApp = (IskApplication) getApplication();
		bitmapManager = iskApp.getBitmapManager();
		
		String characterId = getIntent().getStringExtra("characterID");
		Cursor walletCursor = iskApp.getIskDatabase().getEveWallet(characterId);
		startManagingCursor(walletCursor);
		
		ListAdapter adapter = new WalletAdapter(this, 
				R.layout.wallet_entry, 
				walletCursor);
		setListAdapter(adapter);
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

	class WalletAdapter extends ResourceCursorAdapter {
		
		private DateFormat dateFormatter;
		private NumberFormat numberFormatter;
		private NumberFormat integerFormatter;
		private int positiveNumber;
		private int negativeNumber;
		
		public WalletAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);
			
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
			
			String[] eveReferenceTypes = context.getResources().getStringArray(R.array.eve_reference_types);
			int refType = cursor.getInt(2);
			viewHolder.defaultType.setText(eveReferenceTypes[refType < eveReferenceTypes.length ? refType : 0]);
			
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
