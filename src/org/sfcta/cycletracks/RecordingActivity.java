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
	boolean isRecording = true;
	Intent fi;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recording);

		// Start listening for GPS events
		CycleTrackData ctd = CycleTrackData.get();
		ctd.activity = this;
		ctd.activateListener();

		// Create a notification saying we're recording
		setNotification();

		// Pause button
		final Button pauseButton = (Button) findViewById(R.id.ButtonPause);
		pauseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (isRecording) {
					CycleTrackData.get().killListener();
					pauseButton.setText("Resume");
					RecordingActivity.this.setTitle("CycleTracks - Paused...");
					Toast.makeText(getBaseContext(),"Recording paused; GPS now offline", Toast.LENGTH_LONG).show();
				} else {
					CycleTrackData.get().activateListener();
					pauseButton.setText("Pause");
					RecordingActivity.this.setTitle("CycleTracks - Recording...");
					Toast.makeText(getBaseContext(),"GPS restarted. It may take a moment to resync.", Toast.LENGTH_LONG).show();
				}
				isRecording = !isRecording;
			}
		});

		// Build the finish button and attach it to the finish activity
		final Button finishButton = (Button) findViewById(R.id.ButtonFinished);
		finishButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// If we have points, go to the save-trip activity
				if (CycleTrackData.get().coords.size()>0) {
					fi = new Intent(RecordingActivity.this, SaveTrip.class);

				// Otherwise, cancel and go back to main screen
				} else {
					Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

					// Remove the notification
			    	NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			    	mNotificationManager.cancelAll();

			    	// Go back to main screen
					fi = new Intent(RecordingActivity.this, MainInput.class);
					fi.putExtra("keep", true);
					CycleTrackData.get().killListener();
					CycleTrackData.get().dropTrip();
					CycleTrackData.get().idle=true;
				}

				// Either way, activate next task, and then kill this task
				startActivity(fi);
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
