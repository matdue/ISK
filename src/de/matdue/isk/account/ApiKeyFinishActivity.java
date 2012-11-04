package de.matdue.isk.account;

import java.util.List;

import de.matdue.isk.IskActivity;
import de.matdue.isk.R;
import de.matdue.isk.bitmap.BitmapManager;
import de.matdue.isk.eve.Character;
import de.matdue.isk.eve.EveApi;
import de.matdue.isk.eve.EveApiCacheDummy;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;

public class ApiKeyFinishActivity extends IskActivity {

	private String keyID;
	private String vCode;
	private View progressContainer;
	private View errorContainer;
	private View resultContainer;
	private PilotLoadingTask queryTask;
	private PilotAdapter resultAdapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.apikeyfinish);
        
        Intent intent = getIntent();
        keyID = intent.getStringExtra(Constants.PARAM_KEYID);
        vCode = intent.getStringExtra(Constants.PARAM_VCODE);
        Log.v("ApiKeyFinishActivity", keyID + "/" + vCode);

        progressContainer = findViewById(R.id.apikey_progress_container);
        errorContainer = findViewById(R.id.apikey_error_container);
        resultContainer = findViewById(R.id.apikey_result_container);
        resultAdapter = new PilotAdapter(this, getBitmapManager());
        ListView resultListView = (ListView) findViewById(android.R.id.list);
		resultListView.setAdapter(resultAdapter);
		resultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				onListItemClick((ListView)parent, view, position, id);
			}
		});
        
        // Query API in background
        queryCharacters();
    }
    
    private void queryCharacters() {
    	errorContainer.setVisibility(View.GONE);
    	resultContainer.setVisibility(View.GONE);
    	progressContainer.setVisibility(View.VISIBLE);
    	
    	queryTask = new PilotLoadingTask();
    	queryTask.execute(keyID, vCode);
    }
    
    public void onRepeatClicked(View view) {
    	queryCharacters();
    }
    
    public void onListItemClick(ListView listView, View view, int position, long id) {
    	Character selectedCharacter = (Character) listView.getAdapter().getItem(position);
    	Intent resultIntent = new Intent();
    	resultIntent.putExtra(Constants.PARAM_KEYID, keyID);
    	resultIntent.putExtra(Constants.PARAM_VCODE, vCode);
    	resultIntent.putExtra(Constants.PARAM_CHAR_NAME, selectedCharacter.characterName);
    	resultIntent.putExtra(Constants.PARAM_CHAR_ID, selectedCharacter.characterID);
    	resultIntent.putExtra(Constants.PARAM_CORP_NAME, selectedCharacter.corporationName);
    	resultIntent.putExtra(Constants.PARAM_CORP_ID, selectedCharacter.corporationID);
    	setResult(RESULT_OK, resultIntent);
    	finish();
    }
    
    private class PilotLoadingTask extends AsyncTask<String, Void, List<Character>> {

		@Override
		protected List<Character> doInBackground(String... params) {
			String keyID = params[0];
			String vCode = params[1];
			
			EveApi api = new EveApi(new EveApiCacheDummy());
			List<Character> characters = api.queryCharacters(keyID, vCode);
			api.close();
			
			return characters;
		}
    	
		@Override
		protected void onPostExecute(List<Character> result) {
			if (result == null) {
				animateViewSwitch(progressContainer, errorContainer);
			} else {
				resultAdapter.clear();
				resultAdapter.addAll(result);
				animateViewSwitch(progressContainer, resultContainer);
			}
		}
		
		private void animateViewSwitch(final View viewToHide, final View viewToShow) {
			ViewPropertyAnimator viewToHideAnimator = viewToHide.animate();
			viewToHideAnimator.alpha(0);
			viewToHideAnimator.setListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
				}
				
				@Override
				public void onAnimationRepeat(Animator animation) {
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					viewToHide.setVisibility(View.GONE);
					viewToHide.setAlpha(1);
				}
				
				@Override
				public void onAnimationCancel(Animator animation) {
				}
			});

			ViewPropertyAnimator viewToShowAnimator = viewToShow.animate();
			viewToShow.setAlpha(0);
			viewToShow.setVisibility(View.VISIBLE);
			viewToShowAnimator.alpha(1);
		}
    }
    
    private static class PilotAdapter extends ArrayAdapter<Character> {

    	private BitmapManager bitmapManager;
    	private LayoutInflater inflater;
    	
		public PilotAdapter(Context context, BitmapManager bitmapManager) {
			super(context, R.layout.apikey_entry);
			
			this.bitmapManager = bitmapManager;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View view = convertView;
    		if (view == null) {
    			view = inflater.inflate(R.layout.apikey_entry, parent, false);
    		}
    		
    		ViewHolder viewHolder = (ViewHolder) view.getTag();
    		if (viewHolder == null) {
    			viewHolder = new ViewHolder();
    			viewHolder.image = (ImageView) view.findViewById(R.id.pilot_image);
    			viewHolder.name = (TextView) view.findViewById(R.id.pilot_name);
    			
    			view.setTag(viewHolder);
    		}
    		
    		Character character = getItem(position);
    		String imageUrl = EveApi.getCharacterUrl(character.characterID, 128);
    		bitmapManager.setImageBitmap(viewHolder.image, imageUrl, null, null);
    		viewHolder.name.setText(character.characterName);
    		
    		return view;
    	}
    	
    	class ViewHolder {
    		ImageView image;
    		TextView name;
    	}
    	
    }

}
