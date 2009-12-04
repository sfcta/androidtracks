package org.sfcta.cycletracks;

import java.util.List;
import java.util.Vector;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ShowMap extends MapActivity {
	private MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	ItemizedOverlayTrack gpspoints;
	public static int MAP_EXISTING = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		CycleTrackData ctd = CycleTrackData.get();
		ctd.activity=this;
		
		// Set zoom controls
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapOverlays = mapView.getOverlays();
		
		// Set up the point layer
		Bundle cmds = getIntent().getExtras();
		drawable = getResources().getDrawable(R.drawable.point);
		gpspoints = new ItemizedOverlayTrack(drawable);
		
		// Check if we're building from database.  If so, construct points layer!
		if (cmds != null && cmds.containsKey("showtrip")) {
			ctd.initializeData();

			long tripid = cmds.getLong("showtrip");
			
			// Query the database
	        DbAdapter mDbHelper = new DbAdapter(ShowMap.this);
	        mDbHelper.open();
			Cursor coords = mDbHelper.fetchAllCoordsForTrip(tripid);
			
			
			// Add the GPS points
			int COL_LAT = coords.getColumnIndex("lat");
			int COL_LGT = coords.getColumnIndex("lgt");
			int COL_TIME= coords.getColumnIndex("time");
			
			while (true) {
				int lat = coords.getInt(COL_LAT);
				int lgt = coords.getInt(COL_LGT);
				double time= coords.getDouble(COL_TIME);
				ctd.addPointNow(lat, lgt, time);
				if (coords.isLast()) 
					break;
				coords.moveToNext();
			}
			coords.close();
			mDbHelper.close();
		}
		
		// Find map center and extent
		int latcenter = (ctd.lathigh+ctd.latlow) / 2;
		int lgtcenter = (ctd.lgthigh+ctd.lgtlow) / 2;
		GeoPoint center = new GeoPoint(latcenter,lgtcenter);
		MapController mc = mapView.getController();
		mc.animateTo(center);
		mc.zoomToSpan(ctd.lathigh-ctd.latlow, 
				ctd.lgthigh-ctd.lgtlow);
		// And add the points.
		mapOverlays.add(ctd.gpspoints);
		
		//TODO: This needs to move.  When we get the database set up, erase coords 
		// as soon as it's been uploaded. 
		ctd.initializeData();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// Auto-generated method stub
		return false;
	}

}
