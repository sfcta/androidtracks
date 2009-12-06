package org.sfcta.cycletracks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MainInput extends Activity {
	ArrayList<HashMap> savedtrips = new ArrayList<HashMap>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Let's handle some launcher lifecycle issues:

		// 1. If this activity floated to the top of another CycleTracks task,
		// just kill it. The existing task will reveal itself.
		// (This handles the user hitting HOME and relaunching later.
		if (this.isChild() && this.getIntent().getExtras() == null) {
			this.finish();
		}

		// 2. If we're recording or saving right now, even if this is in a new
		// task, jump to the existing activity.
		// (This handles user hitting BACK button while recording)
		CycleTrackData ctd = CycleTrackData.get();
		if (!ctd.idle) {
			if (ctd.itsTimeToSave) {
				startActivity(new Intent(this, SaveTrip.class));
			} else {
				startActivity(new Intent(this, RecordingActivity.class));
			}
			this.finish();
		}

		// Otherwise we're GTG; build the main screen.
		setContentView(R.layout.main);

		// Switch to user prefs screen if there are no prefs stored yet (first-run)
        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        if (settings.getAll().isEmpty()) {
            startActivity(new Intent(this, UserInfoActivity.class));
        }

		// Set up the list view of saved trips
		ListView listSavedTrips = (ListView) findViewById(R.id.ListSavedTrips);
		populateList(listSavedTrips);
		listSavedTrips
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					public void onItemClick(AdapterView parent, View v, int pos, long id) {
                        CycleTrackData.get().initializeData();

                        Intent i = new Intent(MainInput.this, ShowMap.class);
						i.putExtra("showtrip", id);
						startActivity(i);
					}
				});

		// And set up the record button
		final Button startButton = (Button) findViewById(R.id.ButtonStart);
		final Intent i = new Intent(this, RecordingActivity.class);
		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(i);
				MainInput.this.finish();
			}
		});
	}

	void populateList(ListView lv) {
		// Get list from the real phone database. W00t!
		DbAdapter mDbHelper = new DbAdapter(MainInput.this);
		mDbHelper.open();
		try {
			Cursor allTrips = mDbHelper.fetchAllTrips();

			SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
					R.layout.twolinelist, allTrips, new String[] { "purp",
							"note", "fancystart" }, new int[] {
							R.id.TextView01, R.id.TextView02, R.id.TextView03 });

			lv.setAdapter(sca);
			TextView counter = (TextView) findViewById(R.id.TextViewPreviousTrips);
			int numtrips = allTrips.getCount();
			switch (numtrips) {
			case 0:
				counter.setText("No saved trips.");
				break;
			case 1:
				counter.setText("1 saved trip:");
				break;
			default:
				counter.setText("" + numtrips + " saved trips:");
			}
		} catch (SQLException sqle) {
			// Do nothing, for now!
		}
		mDbHelper.close();
	}
}

class FakeAdapter extends SimpleAdapter {
	public FakeAdapter(Context context, List<? extends Map<String, ?>> data,
			int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}

}
