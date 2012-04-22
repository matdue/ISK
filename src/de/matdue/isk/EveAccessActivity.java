package de.matdue.isk;

import java.text.DateFormat;
import java.util.Date;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class EveAccessActivity extends ListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.eveaccess);
		
		IskApplication iskApp = (IskApplication) getApplication();
		Cursor dataCursor = iskApp.getIskDatabase().getEveApiHistoryCursor();
		startManagingCursor(dataCursor);
		
		ListAdapter adapter = new HistoryAdapter(this, 
				R.layout.eveaccess_entry, 
				dataCursor);
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

	class HistoryAdapter extends ResourceCursorAdapter {
		
		private DateFormat dateFormatter;

		public HistoryAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);
			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// View holder pattern: http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
			// Cache elements instead of looking up using slow findViewById() 
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			if (viewHolder == null) {
				viewHolder = new ViewHolder();
				viewHolder.datetime = (TextView) view.findViewById(R.id.eveaccess_entry_datetime);
				viewHolder.url = (TextView) view.findViewById(R.id.eveaccess_entry_url);
				viewHolder.key_id = (TextView) view.findViewById(R.id.eveaccess_entry_key_id);
				viewHolder.result = (TextView) view.findViewById(R.id.eveaccess_entry_result);
				view.setTag(viewHolder);
			}
			
			// Date and time
			viewHolder.datetime.setText(dateFormatter.format(new Date(cursor.getLong(1))));
			
			// URL
			viewHolder.url.setText(cursor.getString(2));
			
			// Character
			viewHolder.key_id.setText(cursor.getString(3));
			
			// Result
			viewHolder.result.setText(cursor.getString(4));
		}
		
		class ViewHolder {
			TextView datetime;
			TextView url;
			TextView key_id;
			TextView result;
		}
		
	}

}
