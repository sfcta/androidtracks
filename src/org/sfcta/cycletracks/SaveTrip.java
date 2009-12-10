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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SaveTrip extends Activity {
	long tripid;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.save);

		finishRecording();

        // User prefs btn
        final Button prefsButton = (Button) findViewById(R.id.ButtonPrefs);
        final Intent pi = new Intent(this, UserInfoActivity.class);
        prefsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(pi);
            }
        });

        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        if (settings.getAll().size() >= 1) {
            prefsButton.setVisibility(View.GONE);
        }

		// Discard btn
		final Button btnDiscard = (Button) findViewById(R.id.ButtonDiscard);
		btnDiscard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(getBaseContext(), "Trip discarded.",	Toast.LENGTH_SHORT).show();

				cancelRecording();

				Intent i = new Intent(SaveTrip.this, MainInput.class);
				i.putExtra("keepme", true);
				startActivity(i);
				SaveTrip.this.finish();
			}
		});

		// Submit btn
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		btnSubmit.setEnabled(false);
	}

	// submit btn is only activated after the service.finishedRecording() is completed.
	void activateSubmitButton() {
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		final Intent xi = new Intent(this, ShowMap.class);
		btnSubmit.setEnabled(true);

		btnSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				TripData trip = TripData.fetchTrip(SaveTrip.this, tripid);
				trip.populateDetails();

				// Find user-entered info
				Spinner purpose = (Spinner) findViewById(R.id.SpinnerPurp);
				EditText notes = (EditText) findViewById(R.id.NotesField);

				String fancyStartTime = DateFormat.getInstance().format(trip.startTime);

				// "3.5 miles in 26 minutes"
				SimpleDateFormat sdf = new SimpleDateFormat("m");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				String minutes = sdf.format(trip.endTime - trip.startTime);
				String fancyEndInfo = String.format("%1.1f miles, %s minutes.  %s",
						(0.0006212f * trip.distance),
						minutes,
						notes.getEditableText().toString());

				// Save the trip details to the phone database. W00t!
				trip.updateTrip(
						purpose.getSelectedItem().toString(),
						fancyStartTime, fancyEndInfo,
						notes.getEditableText().toString());
				trip.updateTripStatus(TripData.STATUS_COMPLETE);
				resetService();

				// Maybe this is where we should start a new activity for MainInput
				Intent i = new Intent(getApplicationContext(), MainInput.class);
				startActivity(i);

				// Show the map!
                xi.putExtra("showtrip", trip.tripid);
                xi.putExtra("uploadTrip", true);
				startActivity(xi);
				SaveTrip.this.finish();
			}
		});

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

	void resetService() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.reset();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	void finishRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				tripid = rs.finishRecording();
				SaveTrip.this.activateSubmitButton();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}
}
