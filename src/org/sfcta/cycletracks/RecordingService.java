package org.sfcta.cycletracks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class RecordingService extends Service implements LocationListener {
	RecordingActivity recordActivity;
	LocationManager lm = null;
	Location lastLocation;

	int numpoints;
	double latestUpdate;

	DbAdapter mDb;
	TripData trip;

	public static int STATE_IDLE = 0;
	public static int STATE_RECORDING = 1;
	public static int STATE_PAUSED = 2;  //TODO: may not need this one.
	public static int STATE_FULL = 3;

	int state = STATE_IDLE;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	// ---Singleton design pattern! Only one CTD should ever exist.
	private RecordingService() {
	}

	private static class CTDHolder {
		private static final RecordingService INSTANCE = new RecordingService();
	}

	public static RecordingService get() {
		return CTDHolder.INSTANCE;
	}
	// ---End Singleton design pattern.

	// Start "business logic":

	public void startRecording(TripData trip) {
		this.state = STATE_RECORDING;
		this.trip = trip;
		this.numpoints = 0;

		setNotification();

		// Start listening for GPS updates!
        Toast.makeText(this, "Requesting updates", Toast.LENGTH_SHORT).show();
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	public void finishRecording() {
		if (lm != null) {
	        Toast.makeText(this.getBaseContext(), "Stopped listening", Toast.LENGTH_SHORT).show();
			lm.removeUpdates(this);
		}
		clearNotifications();
		this.state = STATE_FULL;
	}

	public void cancelRecording() {
		if (trip != null) {
			trip.dropTrip();
		}

		if (lm != null) {
	        Toast.makeText(this.getBaseContext(), "Cancelling updates", Toast.LENGTH_SHORT).show();
			lm.removeUpdates(this);
		}
		clearNotifications();
		this.state = STATE_IDLE;
	}

	public void registerUpdates(RecordingActivity r) {
		this.recordActivity = r;
	}

	public int getState() {
		return state;
	}

	public TripData getCurrentTrip() {
		return trip;
	}

	// LocationListener implementation:
	@Override
	public void onLocationChanged(Location loc) {
		if (loc != null) {
			// Only save one beep per second.
			double currentTime = System.currentTimeMillis();
			if (currentTime - latestUpdate > 999) {
				trip.addPointNow(loc, currentTime);
				latestUpdate = currentTime;
				numpoints++;
				updateTripStats(loc);

				// Update the status page every time, if we can.
				if (recordActivity != null) {
					recordActivity.updateStatus();
				}
			}
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}
	// END LocationListener implementation:

	private void updateTripStats(Location newLocation) {
	    final float spdConvert = 2.2369f;
	    if (lastLocation != null) {
	        Float segmentDistance = lastLocation.distanceTo(newLocation);
	        trip.distanceTraveled = trip.distanceTraveled.floatValue() + segmentDistance.floatValue();
	        trip.curSpeed = newLocation.getSpeed() * spdConvert;
	        trip.maxSpeed = Math.max(trip.maxSpeed, trip.curSpeed);
            numpoints++;
	    }
	    lastLocation = newLocation;
	}

	private void setNotification() {
		// Create the notification icon - maybe this goes somewhere else?
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.icon25;
		CharSequence tickerText = "Recording...";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = notification.flags
				| Notification.FLAG_ONGOING_EVENT;
		Context context = getApplicationContext();
		CharSequence contentTitle = "CycleTracks - Recording";
		CharSequence contentText = "Tap to finish your trip";
		Intent notificationIntent = new Intent(context, RecordingActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		final int RECORDING_ID = 1;
		mNotificationManager.notify(RECORDING_ID, notification);
	}

	private void clearNotifications() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
	}
}
