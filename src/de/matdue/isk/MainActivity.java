package de.matdue.isk;

import java.math.BigDecimal;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends IskActivity {

	// Dialogs
	private static final int DIALOG_WELCOME = 0;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Spinner spinner = (Spinner) findViewById(R.id.main_pilot);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				//Log.d("onItemSelected", "" + position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				//Log.d("onNothingSelected", "");
			}
		});
        
        List<PilotData> pilots = new ArrayList<PilotData>();
        
        // Sort by character name
 		final Collator collator = Collator.getInstance();
 		Collections.sort(pilots, new Comparator<PilotData>() {
 			@Override
 			public int compare(PilotData lhs, PilotData rhs) {
 				return collator.compare(lhs.name, rhs.name);
 			}
 		});
 		
        PilotAdapter adapter = new PilotAdapter(this, pilots);
        spinner.setAdapter(adapter);
        //spinner.setSelection(1);
        
        showWelcomeDialog();
    }
    
    @Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		Dialog dialog = null;
		switch (id) {
		case DIALOG_WELCOME:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
				.setMessage(R.string.main_dialog_welcome)
				.setNeutralButton(R.string.main_dialog_welcome_close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences preferences = getPreferences();
						preferences
							.edit()
							.putBoolean("welcomed", true)
							.commit();
						dialog.dismiss();
					}
				});
			dialog = builder.create();
			break;
		}
		
		return dialog;
	}
    
    /**
	 * Show a Welcome! dialog
	 */
	private void showWelcomeDialog() {
		SharedPreferences preferences = getPreferences();
		boolean welcomed = preferences.getBoolean("welcomed", false);
		if (!welcomed) {
			showDialog(DIALOG_WELCOME);
		}
	}
	
	private static class PilotData {
		public String characterId;
		public String name;
		public BigDecimal balance;
	}
	
	private class PilotAdapter extends BaseAdapter {
		
		List<PilotData> pilots;
		LayoutInflater inflater;
		NumberFormat balanceFormatter = NumberFormat.getInstance();
		
		public PilotAdapter(Context context, List<PilotData> pilots) {
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.pilots = pilots;
			
			balanceFormatter.setMinimumFractionDigits(2);
			balanceFormatter.setMaximumFractionDigits(2);
		}

		@Override
		public int getCount() {
			return pilots.size();
		}

		@Override
		public Object getItem(int position) {
			return pilots.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getPilotView(position, convertView, parent, R.layout.main_pilot);
		}
		
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getPilotView(position, convertView, parent, R.layout.main_pilot_spinner_item);
		}
		
		private View getPilotView(int position, View convertView, ViewGroup parent, int resource) {
			View view = convertView;
			if (view == null) {
				view = inflater.inflate(resource, parent, false);
			}
			
			ImageView pilotImage = (ImageView) view.findViewById(R.id.main_pilot_image);
			TextView pilotName = (TextView) view.findViewById(R.id.main_pilot_name);
			TextView pilotBalance = (TextView) view.findViewById(R.id.main_pilot_balance);
			
			PilotData pilot = pilots.get(position);
			getBitmapManager().setImageBitmap(pilotImage, "https://image.eveonline.com/Character/" + pilot.characterId + "_128.jpg");
			pilotName.setText(pilot.name);
			if (pilotBalance != null) {
				if (pilot.balance != null) {
					String sBalance = balanceFormatter.format(pilot.balance) + " ISK";
					pilotBalance.setText(sBalance);
				} else {
					pilotBalance.setText("");
				}
			}
			
			return view;
		}
		
	}
	
}