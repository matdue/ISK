package de.matdue.isk;


public class NotificationDeletedReceiver extends WakelockedBroadcastReceiver {

	@Override
	protected Class<? extends WakelockedService> getServiceClass() {
		return NotificationDeletedService.class;
	}

}
