package de.matdue.isk;

import java.util.List;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.database.Character;
import de.matdue.isk.database.IskDatabase;
import de.matdue.isk.eve.EveApi;

public class PilotsActivity extends ExpandableListActivity {
	
	private static final int ApiKeyActivityRequestCode = 0;

	private IskDatabase iskDatabase;
	private BitmapManager bitmapManager;
	
	// For onPause/onResume: remember which groups are expanded, and which are not
	private boolean[] expandedGroups;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		IskApplication iskApp = (IskApplication) getApplication();
		bitmapManager = iskApp.getBitmapManager();
		iskDatabase = iskApp.getIskDatabase();
		Cursor apiKeyCursor = iskDatabase.getApiKeyCursor();
		startManagingCursor(apiKeyCursor);

		PilotsExpandableListAdapter adapter = new PilotsExpandableListAdapter(apiKeyCursor,
				this, android.R.layout.simple_expandable_list_item_1,
				R.layout.expandable_list_item_with_image,
				new String[] { "key" },
				new int[] { android.R.id.text1 }, 
				null, 
				null);
		setListAdapter(adapter);

		int groupCount = adapter.getGroupCount();
		for (int iGroup = 0; iGroup < groupCount; ++iGroup) {
			getExpandableListView().expandGroup(iGroup);
		}

		registerForContextMenu(getExpandableListView());
		
		// If no API key has been added yet, jump to activity to do that
		if (groupCount == 0) {
			startActivityForResult(new Intent(this, ApiKeyActivity.class), ApiKeyActivityRequestCode);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			getMenuInflater().inflate(R.menu.pilots_context, menu);
		}
		String keyID = iskDatabase.getApiKeyID(info.id);
		if (keyID != null) {
			menu.setHeaderTitle("API key " + keyID);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	private void refreshAdapter() {
		Cursor apiKeyCursor = iskDatabase.getApiKeyCursor();
		startManagingCursor(apiKeyCursor);
		PilotsExpandableListAdapter adapter = (PilotsExpandableListAdapter) getExpandableListAdapter();
		Cursor oldCursor = adapter.getCursor();
		adapter.changeCursor(apiKeyCursor);
		adapter.notifyDataSetChanged(true);
		
		stopManagingCursor(oldCursor);
		oldCursor.close();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			switch (item.getItemId()) {
			case R.id.pilots_context_remove:
				iskDatabase.deleteApiKey(info.id);
				refreshAdapter();
				
				// Update current character by removing it, if applicable
				setCurrentCharacter();
				return true;
			}
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.pilots_options, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.pilots_add:
			startActivityForResult(new Intent(this, ApiKeyActivity.class), ApiKeyActivityRequestCode);
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ApiKeyActivityRequestCode && resultCode == RESULT_OK) {
			// New API key added
			String newKeyID = data.getExtras().getString("keyID");
			refreshAdapter();
			
			// Expand the new group and scroll to it
			PilotsExpandableListAdapter adapter = (PilotsExpandableListAdapter) getExpandableListAdapter();
			int groupCount = adapter.getGroupCount();
			for (int iGroup = 0; iGroup < groupCount; ++iGroup) {
				Cursor cursor = adapter.getGroup(iGroup);
				String groupKeyID = cursor.getString(1);
				if (newKeyID.equals(groupKeyID)) {
					ExpandableListView view = getExpandableListView();
					view.expandGroup(iGroup);
					view.smoothScrollToPosition(iGroup);
					break;
				}
			}
			
			// Update all characters
			Intent msgIntent = new Intent(this, EveApiUpdaterService.class);
			msgIntent.putExtra("force", true);
			WakefulIntentService.sendWakefulWork(this, msgIntent);
			
			// Update current character to new API key if applicable
			setCurrentCharacter();
		}
	}
	
	private void setCurrentCharacter() {
		SharedPreferences preferences = getSharedPreferences("de.matdue.isk", MODE_PRIVATE);
		String characterID = preferences.getString("startCharacterID", null);
		List<Character> allCharacters = iskDatabase.queryAllCharacters();
		
		// Take first character as new current character, if none has been defined yet
		if (characterID == null && !allCharacters.isEmpty()) {
			characterID = allCharacters.get(0).characterId;
			Editor editor = preferences.edit();
			editor.putString("startCharacterID", characterID);
			editor.apply();
		}
		// Remove current character if no character is known at all
		else if (characterID != null && allCharacters.isEmpty()) {
			Editor editor = preferences.edit();
			editor.remove("startCharacterID");
			editor.apply();
		}
		// Update current character if it is not known any more
		else if (characterID != null && !allCharacters.isEmpty()) {
			boolean characterIDknown = false;
			for (Character character : allCharacters) {
				if (character.name.equals(characterID)) {
					characterIDknown = true;
					break;
				}
			}
			if (!characterIDknown) {
				Editor editor = preferences.edit();
				editor.remove("startCharacterID");
				editor.apply();
			}
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		CheckBox cb = (CheckBox) v.findViewById(R.id.pilot_checked);
		if (cb != null) {
			cb.toggle();
		}
		return true;
	}
	
	private void saveExpansionState() {
		ExpandableListView view = getExpandableListView();
		PilotsExpandableListAdapter adapter = (PilotsExpandableListAdapter) getExpandableListAdapter();
		int groupCount = adapter.getGroupCount();
		expandedGroups = new boolean[groupCount];
		for (int i = 0; i < groupCount; ++i) {
			expandedGroups[i] = view.isGroupExpanded(i);
		}
	}
	
	private void restoreExpansionState() {
		ExpandableListView view = getExpandableListView();
		PilotsExpandableListAdapter adapter = (PilotsExpandableListAdapter) getExpandableListAdapter();
		int groupCount = adapter.getGroupCount();
		if (expandedGroups != null && expandedGroups.length == groupCount) {
			for (int i = 0; i < groupCount; ++i) {
				if (expandedGroups[i]) {
					view.expandGroup(i);
				} else {
					view.collapseGroup(i);
				}
			}
		}
		expandedGroups = null;  // We don't need it any more
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// Save group expansions
		saveExpansionState();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Restore group expansions
		restoreExpansionState();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save group expansions
		saveExpansionState();
		outState.putBooleanArray("expandedGroups", expandedGroups);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		
		// Restore group expansions
		expandedGroups = state.getBooleanArray("expandedGroups");
		restoreExpansionState();
	}

	public class PilotsExpandableListAdapter extends SimpleCursorTreeAdapter {

		public PilotsExpandableListAdapter(Cursor cursor, Context context,
				int groupLayout, int childLayout, String[] groupFrom,
				int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo,
					childLayout, childrenFrom, childrenTo);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			long apiId = groupCursor.getLong(0);
			Cursor characterCursor = iskDatabase.getCharacterCursor(apiId);

			return characterCursor;
		}

		@Override
		protected void bindChildView(View view, Context context, final Cursor cursor, boolean isLastChild) {
			TextView textView = (TextView) view.findViewById(R.id.pilot_name);
			textView.setText(cursor.getString(1));

			ImageView imageView = (ImageView) view.findViewById(R.id.pilot_image);
			String imageViewUrl = (String) imageView.getTag();
			String imageUrl = EveApi.getCharacterUrl(cursor.getString(3), 128);
			if (!imageUrl.equals(imageViewUrl)) {
				imageView.setTag(imageUrl);
				bitmapManager.setLoadingBitmap(R.drawable.unknown_character_1_128);
				bitmapManager.setImageBitmap(imageView, imageUrl);
			}

			CheckBox checkBox = (CheckBox) view.findViewById(R.id.pilot_checked);
			checkBox.setOnCheckedChangeListener(null);
			checkBox.setChecked(cursor.getInt(2) != 0);
			checkBox.setTag(cursor.getPosition());
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					Integer currentPosition = (Integer) buttonView.getTag();
					if (cursor.moveToPosition(currentPosition)) {
						long id = cursor.getLong(0);
						String uncheckedCharacterID = cursor.getString(3);
						iskDatabase.setCharacterSelection(id, isChecked);

						notifyDataSetChanged(true);
						
						// Make sure deselected character is not shown on start page any more
						if (!isChecked) {
							SharedPreferences preferences = getSharedPreferences("de.matdue.isk", MODE_PRIVATE);
							String characterID = preferences.getString("startCharacterID", null);
							if (uncheckedCharacterID.equals(characterID)) {
								Editor editor = preferences.edit();
								editor.remove("startCharacterID");
								editor.apply();
							}
						}
					}
				}

			});
		}
	}

}
