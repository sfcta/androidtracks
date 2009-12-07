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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MainInput extends Activity {
	ArrayList<HashMap> savedtrips = new ArrayList<HashMap>();
    private final static int MENU_USER_INFO = 0;
    private final static int MENU_RESEND_FAILED_UPLOADS = 1;

    private final static int CONTEXT_RETRY = 0;
    private final static int CONTEXT_DELETE = 1;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Let's handle some launcher lifecycle issues:

		// 1. If this activity floated to the top of another CycleTracks task,
		// just kill it. The existing task will reveal itself.
		// (This handles the user who hit HOME and relaunched later.
		if (this.isChild() && this.getIntent().getExtras() == null) {
			this.finish();
		}

		// 2. If we're recording or saving right now, even if this is in a new
		// task, jump to the existing activity.
		// (This handles user who hit  BACK button while recording)

		//TODO: Open Service, check status:  if not idle, figure out what to do next!
		RecordingService rs = RecordingService.get();
		int state = rs.getState();
		if (state > RecordingService.STATE_IDLE) {
			if (state == RecordingService.STATE_FULL) {
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
/*
		// Clean up any bad trips & coords from crashes
		int cleanedTrips = mDbHelper.cleanTables();
		if (cleanedTrips > 0) {
		    Toast.makeText(getBaseContext(), ""+cleanedTrips+" bad trips discarded.", Toast.LENGTH_SHORT).show();
		}
*/
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

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView parent, View v, int pos, long id) {
		        Intent i = new Intent(MainInput.this, ShowMap.class);
		        i.putExtra("showtrip", id);
		        startActivity(i);
		    }
		});
		registerForContextMenu(lv);
	}

	@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
	        ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    menu.add(0, CONTEXT_RETRY, 0, "Retry Upload");
	    menu.add(0, CONTEXT_DELETE, 0,  "Delete");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	    case CONTEXT_RETRY:
	        retryTripUpload(info.id);
	        return true;
	    case CONTEXT_DELETE:
	        deleteTrip(info.id);
	        return true;
	    default:
	        return super.onContextItemSelected(item);
	    }
	}

	private void retryTripUpload(long tripId) {
	    TripUploader uploader = new TripUploader(getBaseContext());
        uploader.uploadTrip(tripId);
	}

	private void deleteTrip(long tripId) {
	    DbAdapter mDbHelper = new DbAdapter(MainInput.this);
        mDbHelper.open();
        mDbHelper.deleteAllCoordsForTrip(tripId);
        mDbHelper.deleteTrip(tripId);
        mDbHelper.close();
        ListView listSavedTrips = (ListView) findViewById(R.id.ListSavedTrips);
        listSavedTrips.invalidate();
        populateList(listSavedTrips);
    }

	 /* Creates the menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_RESEND_FAILED_UPLOADS, 0, "Retry Failed Uploads").setIcon(android.R.drawable.ic_menu_send);
        menu.add(0, MENU_USER_INFO, 0, "Edit User Info").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_USER_INFO:
            startActivity(new Intent(this, UserInfoActivity.class));
            return true;
        case MENU_RESEND_FAILED_UPLOADS:
            return true;
        }
        return false;
    }
}

class FakeAdapter extends SimpleAdapter {
	public FakeAdapter(Context context, List<? extends Map<String, ?>> data,
			int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}

}
