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

import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class ShowMap extends MapActivity {
	private MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	ItemizedOverlayTrack gpspoints;

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

			int latcenter = (trip.lathigh + trip.latlow) / 2;
			int lgtcenter = (trip.lgthigh + trip.lgtlow) / 2;
			GeoPoint center = new GeoPoint(latcenter, lgtcenter);
			MapController mc = mapView.getController();
			mc.animateTo(center);
			mc.zoomToSpan(trip.lathigh - trip.latlow, trip.lgthigh - trip.lgtlow);

			if (gpspoints == null) {
				AddPointsToMapLayerTask maptask = new AddPointsToMapLayerTask();
				maptask.execute(trip);
			} else {
				mapOverlays.add(gpspoints);
			}

			if (trip.status < TripData.STATUS_SENT
					&& cmds != null
					&& cmds.getBoolean("uploadTrip", false)) {
			    // And upload to the cloud database, too!  W00t W00t!
                TripUploader uploader = new TripUploader(ShowMap.this);
                uploader.execute(trip.tripid);
			}

		} catch (Exception e) {
			Log.e("GOT!",e.toString());
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// Auto-generated method stub
		return false;
	}

	private class AddPointsToMapLayerTask extends AsyncTask <TripData, Integer, ItemizedOverlayTrack> {
		@Override
		protected ItemizedOverlayTrack doInBackground(TripData... trips) {
	        TripData trip = trips[0]; // always get just the first trip

			drawable = getResources().getDrawable(R.drawable.point);
            ShowMap.this.gpspoints = trip.getPoints(drawable);

			return ShowMap.this.gpspoints;
		}

		@Override
		protected void onPostExecute(ItemizedOverlayTrack gpspoints) {
			mapOverlays.add(ShowMap.this.gpspoints);
			mapView.invalidate();
		}
	}
}

