package org.sfcta.cycletracks;

import java.util.List;

import android.graphics.drawable.Drawable;
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

			// Find map center and extent
			int latcenter = (trip.lathigh + trip.latlow) / 2;
			int lgtcenter = (trip.lgthigh + trip.lgtlow) / 2;
			GeoPoint center = new GeoPoint(latcenter, lgtcenter);
			MapController mc = mapView.getController();
			mc.animateTo(center);
			mc.zoomToSpan(trip.lathigh - trip.latlow, trip.lgthigh - trip.lgtlow);

			// *** Fetch the gps points from the TripData
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
}
