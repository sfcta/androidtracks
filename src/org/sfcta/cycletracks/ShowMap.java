package org.sfcta.cycletracks;

import java.util.List;
import java.util.Vector;
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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);
		
		// Set zoom controls
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		
		// Set up the point layer
		mapOverlays = mapView.getOverlays();
		drawable = getResources().getDrawable(R.drawable.point);
		gpspoints = new ItemizedOverlayTrack(drawable);

		// Add the GPS points to it!
		int lathigh,lgthigh,latlow,lgtlow;
		lathigh = (int)(-100 * 1E6);
		latlow  = (int)(100 * 1E6);
		lgtlow  = (int)(360 * 1E6);
		lgthigh = (int)(-360 * 1E6);
		
		for (int i=0; i<CycleTrackData.coords.size(); i++) {
			Location loc = CycleTrackData.coords.get(i);
			int lat = (int)(loc.getLatitude() * 1E6);
			int lgt = (int)(loc.getLongitude() * 1E6);
			latlow = Math.min(latlow,lat);
			lathigh = Math.max(lathigh,lat);
			lgtlow = Math.min(lgtlow,lgt);
			lgthigh = Math.max(lgthigh,lgt);
			
			GeoPoint pt = new GeoPoint(lat,lgt);
			OverlayItem opoint = new OverlayItem(pt,"","");
			gpspoints.addOverlay(opoint);
		}
		MapController mc = mapView.getController();
		mc.animateTo(gpspoints.getItem(0).getPoint());
		mc.zoomToSpan(lathigh-latlow, lgthigh-lgtlow);
		mapOverlays.add(gpspoints);

		//TODO: This needs to move.  When we get the database set up, erase coords 
		// as soon as it's been uploaded. 
		CycleTrackData.coords = new Vector <Location> ();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// Auto-generated method stub
		return false;
	}

}
