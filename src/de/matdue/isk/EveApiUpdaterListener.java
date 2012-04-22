package de.matdue.isk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.commonsware.cwac.wakeful.WakefulIntentService.AlarmListener;

public class EveApiUpdaterListener implements AlarmListener {
	
	@Override
	public long getMaxAge() {
		return AlarmManager.INTERVAL_HOUR * 2;
	}

	@Override
	public void scheduleAlarms(AlarmManager alarmManager, PendingIntent pendingIntent, Context context) {
		String updateInterval = PreferenceManager.getDefaultSharedPreferences(context).getString("updateInterval", "1");
		Log.d("EveApiUpdaterListener", "Update interval: " + updateInterval);
		if (updateInterval != null) {
			try {
				int interval = Integer.parseInt(updateInterval);
				if (interval > 0) {
					alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 
							SystemClock.elapsedRealtime() + 60000, 
							interval * AlarmManager.INTERVAL_HOUR, 
							pendingIntent);
				}
			} catch (NumberFormatException e) {
			}
		}
	}

	@Override
	public void sendWakefulWork(Context context) {
		WakefulIntentService.sendWakefulWork(context, EveApiUpdaterService.class);
	}

}
