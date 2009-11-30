package org.sfcta.cycletracks;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

public class RecordingActivity extends Activity implements LocationListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recording);

		// Create a notification saying we're recording
		setNotification();

		// Start GPS Tracking
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

		// Build the finish btn and attach it to the finish activity
		final Button finishButton = (Button) findViewById(R.id.ButtonFinished);
		final Intent i = new Intent(this, SaveTrip.class);
		finishButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(i);
				RecordingActivity.this.finish();
			}
		});
	}

	private void setNotification() {
		// Create the notification icon - maybe this goes somewhere else?
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.icon;
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

	// LocationListener implementation:
	@Override
	public void onLocationChanged(Location loc) {
		if (loc != null) {
			Toast.makeText(
					getBaseContext(),
					"Location changed : Lat: " + loc.getLatitude() + " Lng: "
							+ loc.getLongitude(), Toast.LENGTH_SHORT).show();

			GeoPoint p = new GeoPoint((int) (loc.getLatitude() * 1E6),
					(int) (loc.getLongitude() * 1E6));
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
	}
	// END LocationListener implementation:

}
