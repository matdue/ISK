package de.matdue.isk;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class WakelockHolder {

	private static WakelockHolder INSTANCE = new WakelockHolder();
	
	private PowerManager.WakeLock wakeLock;
	private int clientCount;
	private int enterCount;

	public static WakelockHolder getInstance() {
		return INSTANCE;
	}
	
	public synchronized void registerClient() {
		++clientCount;
		Log.d("WakelockHolder", "Registered client #" + clientCount);
	}
	
	public synchronized void deregisterClient() {
		Log.d("WakelockHolder", "Deregister client #" + clientCount);
		if (clientCount == 0) {
			return;
		}
		if (--clientCount == 0 && wakeLock != null && wakeLock.isHeld()) {
			enterCount = 0;
			wakeLock.release();
			Log.d("WakelockHolder", "Released wake lock");
		}
	}
	
	public synchronized WakelockHolder prepareEnter(Context context) {
		if (wakeLock == null) {
			Log.d("WakelockHolder", "Create wake lock");
			PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.matdue.isk.WakelockHolder");
			wakeLock.setReferenceCounted(true);
		}
		
		// Acquire at most 1 time, to be able to release it
		// if the last client deregisteres
		if (!wakeLock.isHeld()) {
			wakeLock.acquire();
			Log.d("WakelockHolder", "Acquired wake lock");
		}
		
		return this;
	}
	
	public synchronized void enter() {
		++enterCount;
		Log.d("WakelockHolder", "Entered lock #" + enterCount);
	}
	
	public synchronized void leave() {
		Log.d("WakelockHolder", "Release lock #" + enterCount);
		if (enterCount == 0) {
			return;
		}
		if (--enterCount == 0 && wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			Log.d("WakelockHolder", "Released wake lock");
		}
	}

}
