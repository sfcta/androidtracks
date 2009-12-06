package org.sfcta.cycletracks;

import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.OverlayItem;

public class CycleTrackData implements LocationListener {
	Vector<CyclePoint> coords;
	Activity activity = null;
	LocationManager lm = null;
	Location lastLocation;
	double startTime, latestUpdate;
	Float distanceTraveled = 0.0f;
	ItemizedOverlayTrack gpspoints;
	int lathigh, lgthigh, latlow, lgtlow, latestlat, latestlgt;
	boolean idle = true;
	DbAdapter mDb;
	long tripid;
	boolean itsTimeToSave = false;

	// ---Singleton design pattern! Only one CTD should ever exist.
	private CycleTrackData() {
	}

	private static class CTDHolder {
		private static final CycleTrackData INSTANCE = new CycleTrackData();
	}

	public static CycleTrackData get() {
		return CTDHolder.INSTANCE;
	}

	// ---End Singleton design pattern.

	// If we're just starting to record, set initial conditions.
	void initializeData() {
		coords = new Vector<CyclePoint>();
		startTime = System.currentTimeMillis();
		latestUpdate = latestlat = latestlgt = 0;
		lathigh = (int) (-100 * 1E6);
		latlow = (int) (100 * 1E6);
		lgtlow = (int) (360 * 1E6);
		lgthigh = (int) (-360 * 1E6);
		Drawable drawable = activity.getResources().getDrawable(
				R.drawable.point);
		gpspoints = new ItemizedOverlayTrack(drawable);
	}

	// Start getting updates
	void activateListener() {
		if (idle) {
			idle = false;
			initializeData();

			// Reset database
			try {
				mDb = new DbAdapter(activity);
			} catch (Exception e) {
			}
			mDb.open();
			tripid = mDb.createTrip();
			mDb.close();
		}

        Toast.makeText(activity.getBaseContext(), "Requesting updates", Toast.LENGTH_SHORT).show();
		lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

	}

	void killListener() {
		if (lm != null) {
	        Toast.makeText(activity.getBaseContext(), "Cancelling updates", Toast.LENGTH_SHORT).show();
			lm.removeUpdates(this);
		}
	}

	void dropTrip() {
	    mDb.open();
		mDb.deleteAllCoordsForTrip(tripid);
		mDb.deleteTrip(tripid);
		mDb.close();
		itsTimeToSave = false;
	}

	void addPointNow(Location loc, double currentTime) {
		int lat = (int) (loc.getLatitude() * 1E6);
		int lgt = (int) (loc.getLongitude() * 1E6);
		float accuracy = loc.getAccuracy();
		double altitude = loc.getAltitude();
		float speed = loc.getSpeed();

		// Skip duplicates
		if (latestlat == lat && latestlgt == lgt)
			return;

		CyclePoint pt = new CyclePoint(lat, lgt, currentTime, accuracy,
				altitude, speed);
		coords.add(pt);

		// Only add point to database if we're live (i.e. not just showing a
		// saved map)
		if (!idle)
		    mDb.open();
			mDb.addCoordToTrip(tripid, pt);
			mDb.close();

		OverlayItem opoint = new OverlayItem(pt, "", "");
		gpspoints.addOverlay(opoint);

		latlow = Math.min(latlow, lat);
		lathigh = Math.max(lathigh, lat);
		lgtlow = Math.min(lgtlow, lgt);
		lgthigh = Math.max(lgthigh, lgt);
	}

	void addPointToSavedMap(int lat, int lgt, double currentTime) {
		CyclePoint pt = new CyclePoint(lat, lgt, currentTime);

		OverlayItem opoint = new OverlayItem(pt, "", "");
		gpspoints.addOverlay(opoint);
	}

	// LocationListener implementation:
	@Override
	public void onLocationChanged(Location loc) {
		if (loc != null) {
			// Only save one beep per second.
			double currentTime = System.currentTimeMillis();
			if (currentTime - latestUpdate > 999) {
				addPointNow(loc, currentTime);
				latestUpdate = currentTime;
				updateDistance(loc);
				// Update the status page every time, if we can.
	            //TODO: This should not be here; should be moved to a Listener somewhere
				updateStatus();
			}
		}
	}

	private void updateDistance(Location newLocation) {
	    if (lastLocation != null) {
	        Float segmentDistance = lastLocation.distanceTo(newLocation);
	        distanceTraveled = distanceTraveled.floatValue() + segmentDistance.floatValue();
	    }
	    lastLocation = newLocation;
	}

	private void updateStatus() {
	    if (activity instanceof RecordingActivity) {
            TextView stat = (TextView) activity.findViewById(R.id.TextRecordStats);
            TextView distance = (TextView) activity.findViewById(R.id.TextDistance);
            stat.setText(""+coords.size()+" data points received...");
            distance.setText(String.format("Meters travelled: %1d", distanceTraveled.intValue()));
        }
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// Nothing
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// Nothing
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// Nothing
	}
	// END LocationListener implementation:
}
