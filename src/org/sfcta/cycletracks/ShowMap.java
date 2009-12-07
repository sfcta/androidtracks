package org.sfcta.cycletracks;

import java.util.List;

import android.graphics.drawable.Drawable;
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
	DbAdapter mDbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		try {
			// Set zoom controls
			mapView = (MapView) findViewById(R.id.mapview);
			mapView.setBuiltInZoomControls(true);

			// Set up the point layer
			mapOverlays = mapView.getOverlays();
			if (mapOverlays != null) mapOverlays.clear();

	        Bundle cmds = getIntent().getExtras();
            long tripid = cmds.getLong("showtrip");

            TripData trip = TripData.fetchTrip(this, tripid);
/*
            // Now spawn a helper thread to add the points asynchronously! Whee!
            buildTrackTask = new AddPointsToMapLayerTask();
            buildTrackTask.execute(new Long(tripid));
*/
			// Find map center and extent
			int latcenter = (trip.lathigh + trip.latlow) / 2;
			int lgtcenter = (trip.lgthigh + trip.lgtlow) / 2;
			GeoPoint center = new GeoPoint(latcenter, lgtcenter);
			MapController mc = mapView.getController();
			mc.animateTo(center);
			mc.zoomToSpan(trip.lathigh - trip.latlow, trip.lgthigh - trip.lgtlow);

			// *** Fetch the gps points from the TripData
			Toast.makeText(getBaseContext(),"Loading track...", Toast.LENGTH_SHORT).show();
			drawable = getResources().getDrawable(R.drawable.point);
            ItemizedOverlayTrack gpspoints = trip.getPoints(drawable);

			mapOverlays.add(gpspoints);

		} catch (Exception e) {
			Log.e("GOT!",e.toString());
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		// Auto-generated method stub
		return false;
	}
/*
	private class AddPointsToMapLayerTask extends AsyncTask <Long, Integer, Integer> {
		@Override
		protected Integer doInBackground(Long... tripid) {
	        TripData ctd = TripData.get();
            Integer pk = new Integer(5); // fakey!

            // Only ping the database if we don't already have all the gpspoints.
            if (ctd.gpspoints.size()==0) {
            	try {
                    mDbHelper = new DbAdapter(ShowMap.this);
                    mDbHelper.open();
    	            Cursor coords = mDbHelper.fetchAllCoordsForTrip(tripid[0].longValue());
    	            int COL_LAT = coords.getColumnIndex("lat");
    	            int COL_LGT = coords.getColumnIndex("lgt");
    	            int COL_TIME = coords.getColumnIndex("time");

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
    	            mDbHelper.close();
            	} catch (Exception e) {
            		// This can happen if the task is killed (rotated) and the
            		// db connection is lost.
            		Log.e("INFO","DB Connection lost, giving up...");
            		e.printStackTrace();
            	}
            }
			return pk;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
//			mapView.invalidate();
		}

		@Override
		protected void onPostExecute(Integer i) {
			try {
				if (mapOverlays.isEmpty()) {
					mapOverlays.add(TripData.get().gpspoints);
				}
				mapView.invalidate();
			} catch (Exception e) {
				Log.e("GOT!",e.toString());
			}

		}
	}
*/
}
