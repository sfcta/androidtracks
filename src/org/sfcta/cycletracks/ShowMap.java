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
//		drawable = getResources().getDrawable(R.drawable.point);
//		gpspoints = new ItemizedOverlayTrack(drawable);

		// Add the GPS points to it!
/*		for (CyclePoint pt : CycleTrackData.coords) {
			OverlayItem opoint = new OverlayItem(pt,"","");
			gpspoints.addOverlay(opoint);
		}
*/		
		// Find map center and extent
		CycleTrackData ctd = CycleTrackData.getInstance();
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
