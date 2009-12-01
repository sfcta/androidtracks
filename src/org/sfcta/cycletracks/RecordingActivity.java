package org.sfcta.cycletracks;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class RecordingActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recording);

		// Start listening for GPS events
		CycleTrackData.activity = this;
		CycleTrackData.activateListener();
		
		// Create a notification saying we're recording
		setNotification();

		// Build the finish button and attach it to the finish activity
		final Button finishButton = (Button) findViewById(R.id.ButtonFinished);
		final Intent i = new Intent(this, SaveTrip.class);
		finishButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(i);
				RecordingActivity.this.finish();
			}
		});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
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

}
