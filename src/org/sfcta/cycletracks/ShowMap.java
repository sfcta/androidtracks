package org.sfcta.cycletracks;

import java.text.DateFormat;
import java.util.List;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class ShowMap extends MapActivity {
	private MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	//ItemizedOverlayTrack gpspoints;
	DbAdapter mDbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		CycleTrackData ctd = CycleTrackData.get();
		ctd.activity = this;

		// Set zoom controls
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		// Set up the point layer
		mapOverlays = mapView.getOverlays();
		if (mapOverlays != null) mapOverlays.clear();
		
		drawable = getResources().getDrawable(R.drawable.point);
		
		// Check if we're building from database. If so, construct points layer!
		Bundle cmds = getIntent().getExtras();
		if (cmds != null && cmds.containsKey("showtrip")) {
			ctd.initializeData();

			long tripid = cmds.getLong("showtrip");

			// Query the database
			mDbHelper = new DbAdapter(ShowMap.this);
			mDbHelper.open();
			
			// Get lat/long extremes, etc, from trip record
			Cursor tripdetails = mDbHelper.fetchTrip(tripid);
			ctd.lathigh = tripdetails.getInt(tripdetails.getColumnIndex("lathi")); 
			ctd.latlow =  tripdetails.getInt(tripdetails.getColumnIndex("latlo")); 
			ctd.lgthigh = tripdetails.getInt(tripdetails.getColumnIndex("lgthi")); 
			ctd.lgtlow =  tripdetails.getInt(tripdetails.getColumnIndex("lgtlo"));
			tripdetails.close();

			// Add the GPS points
			Toast.makeText(getBaseContext(),"Loading track...", Toast.LENGTH_SHORT).show();

			// Now spawn a helper thread to add the points asynchronously! Whee!
			new AddPointsToMapLayerTask().execute(new Long(tripid));			
		}

		// Find map center and extent
		int latcenter = (ctd.lathigh + ctd.latlow) / 2;
		int lgtcenter = (ctd.lgthigh + ctd.lgtlow) / 2;
		GeoPoint center = new GeoPoint(latcenter, lgtcenter);
		MapController mc = mapView.getController();
		mc.animateTo(center);
		mc.zoomToSpan(ctd.lathigh - ctd.latlow, ctd.lgthigh - ctd.lgtlow);
		
		// Show the points if there's no background task.
		if (cmds == null) mapOverlays.add(ctd.gpspoints);
		
		// This lets the gps listener know we're done collecting data; any
		// future data can start a new (re-initialized) path.
		ctd.idle = true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// Auto-generated method stub
		return false;
	}

	private class AddPointsToMapLayerTask extends AsyncTask <Long, Integer, Integer> {
		int updates = 0;
		
		@Override
		protected Integer doInBackground(Long... tripid) {
			Integer pk = new Integer(5); // fakey!
			Cursor coords = mDbHelper.fetchAllCoordsForTrip(tripid[0].longValue());
			int COL_LAT = coords.getColumnIndex("lat");
			int COL_LGT = coords.getColumnIndex("lgt");
			int COL_TIME = coords.getColumnIndex("time");
			CycleTrackData ctd = CycleTrackData.get();

			while (true) {
				int lat = coords.getInt(COL_LAT);
				int lgt = coords.getInt(COL_LGT);
				double time = coords.getDouble(COL_TIME);
				
				ctd.addPointToSavedMap(lat, lgt, time);
				if (coords.isLast()) break;
				
				if (ctd.gpspoints.size() % 100 == 99) publishProgress(pk);
				
				coords.moveToNext();
			}
			coords.close();
			return pk;
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
//			mapView.invalidate(); 
		}
		
		@Override
		protected void onPostExecute(Integer i) {
			if (mapOverlays.isEmpty()) {
				mapOverlays.add(CycleTrackData.get().gpspoints);
			}
			mapView.invalidate();
		}
		
	}
}
