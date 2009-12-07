package org.sfcta.cycletracks;

import java.text.DateFormat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SaveTrip extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.save);

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

				RecordingService.get().cancelRecording();

				Intent i = new Intent(SaveTrip.this, MainInput.class);
				i.putExtra("keepme", true);
				startActivity(i);
				SaveTrip.this.finish();
			}
		});

		// Submit btn
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		final Intent xi = new Intent(this, ShowMap.class);

		btnSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				RecordingService rs = RecordingService.get();

				Toast.makeText(getBaseContext(),
				        "Submitting trip with "+rs.numpoints+" points. Thanks for using CycleTracks!",
				        Toast.LENGTH_SHORT).show();

				// Find user-entered info
				Spinner purpose = (Spinner) findViewById(R.id.SpinnerPurp);
				EditText notes = (EditText) findViewById(R.id.NotesField);

				TripData trip = RecordingService.get().getCurrentTrip();
				String fancystarttime = DateFormat.getInstance().format(trip.startTime);

				// Save the trip coords to the phone database. W00t!
				DbAdapter mDbHelper = new DbAdapter(SaveTrip.this);
				mDbHelper.open();
				mDbHelper.updateTrip(trip.tripid, purpose.getSelectedItem().toString(),
						trip.startTime, fancystarttime, notes.getEditableText().toString(),
						trip.lathigh, trip.latlow, trip.lgthigh, trip.lgtlow);
				mDbHelper.close();

				// And upload to the cloud database, too!  W00t W00t!
				TripUploader uploader = new TripUploader(getBaseContext());
                uploader.uploadTrip(trip.tripid);

				// Show the map!
                xi.putExtra("showtrip", trip.tripid);
				startActivity(xi);
				SaveTrip.this.finish();
			}
		});
	}
}
