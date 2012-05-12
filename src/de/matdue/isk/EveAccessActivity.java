package de.matdue.isk;

import java.text.DateFormat;
import java.util.Date;

import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class EveAccessActivity extends IskActivity {
	
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
	
	public class CursorLoaderListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
		
		private HistoryAdapter adapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			// Give some text to display if there is no data.
			setEmptyText(getResources().getText(R.string.eveaccess_no_data));
			
			adapter = new HistoryAdapter(getActivity(), 
					R.layout.eveaccess_entry, 
					null);
			setListAdapter(adapter);
			
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
					return getDatabase().getEveApiHistoryCursor();
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
		
	}
	
	static class HistoryAdapter extends ResourceCursorAdapter {
		
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
		
		static class ViewHolder {
			TextView datetime;
			TextView url;
			TextView key_id;
			TextView result;
		}
		
	}

}
