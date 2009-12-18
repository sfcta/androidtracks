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
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
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
	Timer timer;
	float curDistance;

    TextView txtStat;
    TextView txtDistance;
    TextView txtDuration;
    TextView txtCurSpeed;
    TextView txtMaxSpeed;
    TextView txtAvgSpeed;

    final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");

    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    final Runnable mUpdateTimer = new Runnable() {
        public void run() {
            updateTimer();
        }
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.recording);

        txtStat =     (TextView) findViewById(R.id.TextRecordStats);
        txtDistance = (TextView) findViewById(R.id.TextDistance);
        txtDuration = (TextView) findViewById(R.id.TextDuration);
        txtCurSpeed = (TextView) findViewById(R.id.TextSpeed);
        txtMaxSpeed = (TextView) findViewById(R.id.TextMaxSpeed);
        txtAvgSpeed = (TextView) findViewById(R.id.TextAvgSpeed);

		pauseButton = (Button) findViewById(R.id.ButtonPause);
		finishButton = (Button) findViewById(R.id.ButtonFinished);

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

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
					// Don't include pause time in trip duration
					if (trip.pauseStartedAt > 0) {
	                    trip.totalPauseTime += (System.currentTimeMillis() - trip.pauseStartedAt);
	                    trip.pauseStartedAt = 0;
					}
					Toast.makeText(getBaseContext(),"GPS restarted. It may take a moment to resync.", Toast.LENGTH_LONG).show();
				} else {
					pauseButton.setText("Resume");
					RecordingActivity.this.setTitle("CycleTracks - Paused...");
					trip.pauseStartedAt = System.currentTimeMillis();
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
				    // Handle pause time gracefully
                    if (trip.pauseStartedAt> 0) {
                        trip.totalPauseTime += (System.currentTimeMillis() - trip.pauseStartedAt);
                    }
                    if (trip.totalPauseTime > 0) {
                        trip.endTime = System.currentTimeMillis() - trip.totalPauseTime;
                    }
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

	public void updateStatus(int points, float distance, float spdCurrent, float spdMax) {
	    this.curDistance = distance;

	    //TODO: check task status before doing this?
        if (points>0) {
            txtStat.setText(""+points+" data points received...");
        } else {
            txtStat.setText("Waiting for GPS fix...");
        }
        txtCurSpeed.setText(String.format("%1.1f mph", spdCurrent));
        txtMaxSpeed.setText(String.format("Max Speed: %1.1f mph", spdMax));

    	float miles = 0.0006212f * distance;
    	txtDistance.setText(String.format("%1.1f miles", miles));
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
		// This should block until the onServiceConnected (above) completes, but doesn't
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

	// onResume is called whenever this activity comes to foreground.
	// Use a timer to update the trip duration.
    @Override
    public void onResume() {
        super.onResume();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(mUpdateTimer);
            }
        }, 0, 1000);  // every second
    }

    void updateTimer() {
        if (trip != null && isRecording) {
            double dd = System.currentTimeMillis()
                        - trip.startTime
                        - trip.totalPauseTime;

            txtDuration.setText(sdf.format(dd));

            double avgSpeed = 3600.0 * 0.6212 * this.curDistance / dd;
            txtAvgSpeed.setText(String.format("%1.1f mph", avgSpeed));
        }
    }

    // Don't do pointless UI updates if the activity isn't being shown.
    @Override
    public void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }
}
