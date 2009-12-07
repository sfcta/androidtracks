package org.sfcta.cycletracks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RecordingActivity extends Activity {
	Intent fi;
	TripData trip;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recording);

		RecordingService rs = RecordingService.get();

		// If service is idle, start recording!
		if (rs.getState() == RecordingService.STATE_IDLE) {
			trip = TripData.createTrip(this);
			rs.startRecording(trip);
			rs.registerUpdates(this);
		}
/*
		// Pause button
		final Button pauseButton = (Button) findViewById(R.id.ButtonPause);
		pauseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (isRecording) {
					// TripData.get().killListener();
					pauseButton.setText("Resume");
					RecordingActivity.this.setTitle("CycleTracks - Paused...");
					Toast.makeText(getBaseContext(),"Recording paused; GPS now offline", Toast.LENGTH_LONG).show();
				} else {
					// TripData.get().activateListener();
					pauseButton.setText("Pause");
					RecordingActivity.this.setTitle("CycleTracks - Recording...");
					Toast.makeText(getBaseContext(),"GPS restarted. It may take a moment to resync.", Toast.LENGTH_LONG).show();
				}
				isRecording = !isRecording;
			}
		});
*/
		// Finish button
		final Button finishButton = (Button) findViewById(R.id.ButtonFinished);

		finishButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// If we have points, go to the save-trip activity
				if (trip.dirty) {
					fi.putExtra("trip", trip.tripid);
					fi = new Intent(RecordingActivity.this, SaveTrip.class);

				// Otherwise, cancel and go back to main screen
				} else {
					Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

					// Cancel the recording
					RecordingService.get().cancelRecording();
					//TODO: don't need this anymore:  TripData.get().dropTrip();

			    	// Go back to main screen
					fi = new Intent(RecordingActivity.this, MainInput.class);
					fi.putExtra("keep", true);
				}

				// Either way, activate next task, and then kill this task
				startActivity(fi);
				RecordingActivity.this.finish();
			}
		});
	}

	public void updateStatus() {
	    //TODO: check task status before doing this
        TextView stat = (TextView) findViewById(R.id.TextRecordStats);
        TextView distance = (TextView) findViewById(R.id.TextDistance);
        TextView txtCurSpeed = (TextView) findViewById(R.id.TextSpeed);
        TextView txtMaxSpeed = (TextView) findViewById(R.id.TextMaxSpeed);

        stat.setText(""+trip.numpoints+" data points received...");
        distance.setText(String.format("Meters travelled: %1d", trip.distanceTraveled.intValue()));
        txtCurSpeed.setText(String.format("Current speed: %1.1f", trip.curSpeed));
        txtMaxSpeed.setText(String.format("Maximum speed: %1.1f", trip.maxSpeed));
	}
}
