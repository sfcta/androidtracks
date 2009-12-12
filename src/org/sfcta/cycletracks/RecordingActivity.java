/**	 CycleTracks, (c) 2009 San Francisco County Transportation Authority
 * 					  San Francisco, CA, USA
 *
 *   Licensed under the GNU GPL version 3.0.
 *   See http://www.gnu.org/licenses/gpl-3.0.txt for a copy of GPL version 3.0.
 *
 * 	 @author Billy Charlton <billy.charlton@sfcta.org>
 *
 */
package org.sfcta.cycletracks;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RecordingActivity extends Activity {
	Intent fi;
	TripData trip;
	boolean isRecording = false;
	Button pauseButton;
	Button finishButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.recording);
		pauseButton = (Button) findViewById(R.id.ButtonPause);
		finishButton = (Button) findViewById(R.id.ButtonFinished);

		// Query the RecordingService to figure out what to do.
		Intent rService = new Intent(this, RecordingService.class);
		startService(rService);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;

				switch (rs.getState()) {
					case RecordingService.STATE_IDLE:
						trip = TripData.createTrip(RecordingActivity.this);
						rs.startRecording(trip);
						isRecording = true;
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.setTitle("CycleTracks - Recording...");
						break;
					case RecordingService.STATE_RECORDING:
						long id = rs.getCurrentTrip();
						trip = TripData.fetchTrip(RecordingActivity.this, id);
						isRecording = true;
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.setTitle("CycleTracks - Recording...");
						break;
					case RecordingService.STATE_PAUSED:
						long tid = rs.getCurrentTrip();
						isRecording = false;
						trip = TripData.fetchTrip(RecordingActivity.this, tid);
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.pauseButton.setText("Resume");
						RecordingActivity.this.setTitle("CycleTracks - Paused...");
						break;
					case RecordingService.STATE_FULL:
						// Should never get here, right?
						break;
				}
				rs.setListener(RecordingActivity.this);
				unbindService(this);
			}
		};
		bindService(rService, sc, Context.BIND_AUTO_CREATE);

		// Pause button
		pauseButton.setEnabled(false);
		pauseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				isRecording = !isRecording;
				if (isRecording) {
					pauseButton.setText("Pause");
					RecordingActivity.this.setTitle("CycleTracks - Recording...");
					Toast.makeText(getBaseContext(),"GPS restarted. It may take a moment to resync.", Toast.LENGTH_LONG).show();
				} else {
					pauseButton.setText("Resume");
					RecordingActivity.this.setTitle("CycleTracks - Paused...");
					Toast.makeText(getBaseContext(),"Recording paused; GPS now offline", Toast.LENGTH_LONG).show();
				}
				RecordingActivity.this.setListener();
			}
		});

		// Finish button
		finishButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// If we have points, go to the save-trip activity
				if (trip.numpoints > 0) {
					// Save trip so far (points and extent, but no purpose or notes)
					fi = new Intent(RecordingActivity.this, SaveTrip.class);
					trip.updateTrip("","","","");
				}
				// Otherwise, cancel and go back to main screen
				else {
					Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

					cancelRecording();

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

	public void updateStatus(int points, float distance, double duration, float spdCurrent, float spdMax) {
        final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

	    //TODO: check task status before doing this
        TextView txtStat = (TextView) findViewById(R.id.TextRecordStats);
        TextView txtDistance = (TextView) findViewById(R.id.TextDistance);
        TextView txtDuration = (TextView) findViewById(R.id.TextDuration);
        TextView txtCurSpeed = (TextView) findViewById(R.id.TextSpeed);
        TextView txtMaxSpeed = (TextView) findViewById(R.id.TextMaxSpeed);
        if (points>0) {
            txtStat.setText(""+points+" data points received...");
        } else {
            txtStat.setText("Waiting for GPS fix...");
        }
        txtCurSpeed.setText(String.format("Current speed: %1.1f mph", spdCurrent));
        txtMaxSpeed.setText(String.format("Maximum speed: %1.1f mph", spdMax));

        txtDuration.setText(String.format("Time Elapsed: %ss", sdf.format(duration)));

        // Distance funky!
    	float miles = 0.0006212f * distance;
    	txtDistance.setText(String.format("Distance traveled: %1.1f miles", miles));
	}

	void setListener() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				if (RecordingActivity.this.isRecording) {
					rs.resumeRecording();
				} else {
					rs.pauseRecording();
				}
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	void cancelRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.cancelRecording();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}
}
