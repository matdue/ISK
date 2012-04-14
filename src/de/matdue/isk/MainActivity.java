package de.matdue.isk;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

public class MainActivity extends IskActivity {

	// Dialogs
	private static final int DIALOG_WELCOME = 0;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
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
	
}