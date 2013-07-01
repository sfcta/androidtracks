/**	 CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 *                                    San Francisco, CA, USA
 *
 * 	 @author Billy Charlton <billy.charlton@sfcta.org>
 *
 *   This file is part of CycleTracks.
 *
 *   CycleTracks is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   CycleTracks is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with CycleTracks.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.openbike.cycletracks;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class RecordingService extends Service implements LocationListener {
	RecordingActivity recordActivity;
	LocationManager lm = null;
	DbAdapter mDb;

	// Bike bell variables
	static int BELL_FIRST_INTERVAL = 20;
	static int BELL_NEXT_INTERVAL = 5;
    Timer timer;
	SoundPool soundpool;
	int bikebell;
    final Handler mHandler = new Handler();
    final Runnable mRemindUser = new Runnable() {
        public void run() { remindUser(); }
    };

	// Aspects of the currently recording trip
	double latestUpdate;
	Location lastLocation;
	float distanceTraveled;
	float curSpeed, maxSpeed;
	TripData trip;

	public final static int STATE_IDLE = 0;
	public final static int STATE_RECORDING = 1;
	public final static int STATE_PAUSED = 2;
	public final static int STATE_FULL = 3;

	int state = STATE_IDLE;
	private final MyServiceBinder myServiceBinder = new MyServiceBinder();

	// ---SERVICE methods - required! -----------------
	@Override
	public IBinder onBind(Intent arg0) {
		return myServiceBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	    soundpool = new SoundPool(1,AudioManager.STREAM_NOTIFICATION,0);
	    bikebell = soundpool.load(this.getBaseContext(), R.raw.bikebell,1);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
            if (timer!=null) {
                timer.cancel();
                timer.purge();
            }
	}

	public class MyServiceBinder extends Binder implements IRecordService {
		public int getState() {
			return state;
		}
		public void startRecording(TripData trip) {
			RecordingService.this.startRecording(trip);
		}
		public void cancelRecording() {
			RecordingService.this.cancelRecording();
		}
		public void pauseRecording() {
			RecordingService.this.pauseRecording();
		}
		public void resumeRecording() {
			RecordingService.this.resumeRecording();
		}
		public long finishRecording() {
			return RecordingService.this.finishRecording();
		}
		public long getCurrentTrip() {
			if (RecordingService.this.trip != null) {
				return RecordingService.this.trip.tripid;
			}
			return -1;
		}
		public void reset() {
			RecordingService.this.state = STATE_IDLE;
		}
		public void setListener(RecordingActivity ra) {
			RecordingService.this.recordActivity = ra;
			notifyListeners();
		}
	}

	// ---end SERVICE methods -------------------------

	public void startRecording(TripData trip) {
		this.state = STATE_RECORDING;
		this.trip = trip;

	    curSpeed = maxSpeed = distanceTraveled = 0.0f;
	    lastLocation = null;

	    // Add the notify bar and blinking light
		setNotification();

		// Start listening for GPS updates!
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

		// Set up timer for bike bell
		if (timer != null) {
		    timer.cancel(); timer.purge();
		}
        timer = new Timer();
        timer.schedule (new TimerTask() {
            @Override public void run() {
                mHandler.post(mRemindUser);
            }
        }, BELL_FIRST_INTERVAL*60000, BELL_NEXT_INTERVAL*60000);
	}

	public void pauseRecording() {
		this.state = STATE_PAUSED;
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(this);
	}

	public void resumeRecording() {
		this.state = STATE_RECORDING;
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	public long finishRecording() {
		this.state = STATE_FULL;
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(this);

		clearNotifications();

		return trip.tripid;
	}

	public void cancelRecording() {
		if (trip != null) {
			trip.dropTrip();
		}

		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(this);

		clearNotifications();
		this.state = STATE_IDLE;
	}

	public void registerUpdates(RecordingActivity r) {
		this.recordActivity = r;
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

				latestUpdate = currentTime;
				updateTripStats(loc);
				boolean rtn = trip.addPointNow(loc, currentTime, distanceTraveled);
				if (!rtn) {
	                Log.e("FAIL", "Couldn't write to DB");
				}

				// Update the status page every time, if we can.
				notifyListeners();
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

	public void remindUser() {
	    soundpool.play(bikebell, 1.0f, 1.0f, 1, 0, 1.0f);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.icon25;
        long when = System.currentTimeMillis();
        int minutes = (int) (when - trip.startTime) / 60000;
		CharSequence tickerText = String.format("Still recording (%d min)", minutes);

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |=
				Notification.FLAG_ONGOING_EVENT |
				Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = 0xffff00ff;
		notification.ledOnMS = 300;
		notification.ledOffMS = 3000;

		Context context = this;
		CharSequence contentTitle = "OpenBike - Recording";
		CharSequence contentText = "Tap to finish your trip";
		Intent notificationIntent = new Intent(context, RecordingActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,	contentIntent);
        final int RECORDING_ID = 1;
		mNotificationManager.notify(RECORDING_ID, notification);
	}

	private void setNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.icon25;
		CharSequence tickerText = "Recording...";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		notification.ledARGB = 0xffff00ff;
		notification.ledOnMS = 300;
		notification.ledOffMS = 3000;
		notification.flags = notification.flags |
				Notification.FLAG_ONGOING_EVENT |
				Notification.FLAG_SHOW_LIGHTS |
				Notification.FLAG_INSISTENT |
				Notification.FLAG_NO_CLEAR;

		Context context = this;
		CharSequence contentTitle = "OpenBike - Recording";
		CharSequence contentText = "Tap to finish your trip";
		Intent notificationIntent = new Intent(context, RecordingActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,	contentIntent);
		final int RECORDING_ID = 1;
		mNotificationManager.notify(RECORDING_ID, notification);
	}

	private void clearNotifications() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();

		if (timer!=null) {
            timer.cancel();
            timer.purge();
		}
	}

    private void updateTripStats(Location newLocation) {
        final float spdConvert = 2.2369f;

    	// Stats should only be updated if accuracy is decent
    	if (newLocation.getAccuracy()< 20) {
            // Speed data is sometimes awful, too:
            curSpeed = newLocation.getSpeed() * spdConvert;
            if (curSpeed < 60.0f) {
            	maxSpeed = Math.max(maxSpeed, curSpeed);
            }
            if (lastLocation != null) {
                float segmentDistance = lastLocation.distanceTo(newLocation);
                distanceTraveled = distanceTraveled + segmentDistance;
            }

            lastLocation = newLocation;
    	}
    }

    void notifyListeners() {
    	if (recordActivity != null) {
    		recordActivity.updateStatus(trip.numpoints, distanceTraveled, curSpeed, maxSpeed);
    	}
    }
}
