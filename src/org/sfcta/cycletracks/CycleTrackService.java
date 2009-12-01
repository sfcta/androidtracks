package org.sfcta.cycletracks;

import java.util.Vector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class CycleTrackService extends Service implements LocationListener {
	public Vector <Location> coords;
	private LocationManager lm;
	
	@Override
	public void onCreate() {
		super.onCreate();
		coords = new Vector <Location> ();
		Toast.makeText(getBaseContext(),"Service onCreate", Toast.LENGTH_SHORT).show();

		// Start GPS Tracking
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// LocationListener implementation:
	@Override
	public void onLocationChanged(Location loc) {

		if (loc != null) {
			Toast.makeText(getBaseContext(),"Location changed!", Toast.LENGTH_SHORT).show();
			
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
