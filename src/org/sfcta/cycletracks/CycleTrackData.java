package org.sfcta.cycletracks;

import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class CycleTrackData implements LocationListener {
	public static Vector <Location> coords = new Vector <Location> ();
	public static CycleTrackData ctd = null;
	public static Activity activity = null;
	public static LocationManager lm = null;
	
	public static void activateListener() {
		lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
		if (ctd == null) ctd = new CycleTrackData();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ctd);
	}
	
	public static void killListener() {
		lm.removeUpdates(ctd);
	}
	
	// LocationListener implementation:
	@Override
	public void onLocationChanged(Location loc) {
		if (loc != null) {
			// Do stuff!
			coords.add(loc);
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
	}
	// END LocationListener implementation:	

}
