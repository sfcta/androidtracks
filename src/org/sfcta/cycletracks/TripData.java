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

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class TripData {
	long tripid;
	double startTime = 0;
	double endTime = 0;
	int numpoints = 0;
	int lathigh, lgthigh, latlow, lgtlow, latestlat, latestlgt;
	int status;
	float distance;
	String purp, fancystart, info;
	private ItemizedOverlayTrack gpspoints;
	GeoPoint startpoint, endpoint;
	double totalPauseTime = 0;
	double pauseStartedAt = 0;

	DbAdapter mDb;

    public static int STATUS_INCOMPLETE = 0;
    public static int STATUS_COMPLETE = 1;
    public static int STATUS_SENT = 2;

	public static TripData createTrip(Context c) {
		TripData t = new TripData(c.getApplicationContext(), 0);
		t.createTripInDatabase(c);
        t.initializeData();
		return t;
	}

	public static TripData fetchTrip(Context c, long tripid) {
		TripData t = new TripData(c.getApplicationContext(), tripid);
		t.populateDetails();
		return t;
	}

	public TripData (Context ctx, long tripid) {
		Context context = ctx.getApplicationContext();
		this.tripid = tripid;
		mDb = new DbAdapter(context);
	}

	void initializeData() {
		startTime = System.currentTimeMillis();
		endTime = System.currentTimeMillis();
        numpoints = 0;
        latestlat = 800; latestlgt = 800;
        distance = 0;

        lathigh = (int) (-100 * 1E6);
		latlow = (int) (100 * 1E6);
		lgtlow = (int) (180 * 1E6);
		lgthigh = (int) (-180 * 1E6);
		purp = fancystart = info = "";

		updateTrip();
	}

    // Get lat/long extremes, etc, from trip record
	void populateDetails() {

	    mDb.openReadOnly();

	    Cursor tripdetails = mDb.fetchTrip(tripid);
	    startTime = tripdetails.getDouble(tripdetails.getColumnIndex("start"));
	    lathigh = tripdetails.getInt(tripdetails.getColumnIndex("lathi"));
	    latlow =  tripdetails.getInt(tripdetails.getColumnIndex("latlo"));
	    lgthigh = tripdetails.getInt(tripdetails.getColumnIndex("lgthi"));
	    lgtlow =  tripdetails.getInt(tripdetails.getColumnIndex("lgtlo"));
	    status =  tripdetails.getInt(tripdetails.getColumnIndex("status"));
	    endTime = tripdetails.getDouble(tripdetails.getColumnIndex("endtime"));
        distance = tripdetails.getFloat(tripdetails.getColumnIndex("distance"));

        purp = tripdetails.getString(tripdetails.getColumnIndex("purp"));
        fancystart = tripdetails.getString(tripdetails.getColumnIndex("fancystart"));
        info = tripdetails.getString(tripdetails.getColumnIndex("fancyinfo"));

	    tripdetails.close();

		Cursor points = mDb.fetchAllCoordsForTrip(tripid);
		if (points != null) {
	        numpoints = points.getCount();
	        points.close();
		}

	    mDb.close();
	}

	void createTripInDatabase(Context c) {
		mDb.open();
		tripid = mDb.createTrip();
		mDb.close();
	}

	void dropTrip() {
	    mDb.open();
		mDb.deleteAllCoordsForTrip(tripid);
		mDb.deleteTrip(tripid);
		mDb.close();
	}

	public ItemizedOverlayTrack getPoints(Drawable d) {
		// If already built, don't build again!
		if (gpspoints != null && gpspoints.size()>0) {
			return gpspoints;
		}

		// Otherwise, we need to query DB and build points from scratch.
		gpspoints = new ItemizedOverlayTrack(d);

		try {
			mDb.openReadOnly();

			Cursor points = mDb.fetchAllCoordsForTrip(tripid);
            int COL_LAT = points.getColumnIndex("lat");
            int COL_LGT = points.getColumnIndex("lgt");
            int COL_TIME = points.getColumnIndex("time");
            numpoints = points.getCount();

            points.moveToLast();
            this.endpoint   = new CyclePoint(points.getInt(COL_LAT), points.getInt(COL_LGT), points.getDouble(COL_TIME));

            points.moveToFirst();
            this.startpoint = new CyclePoint(points.getInt(COL_LAT), points.getInt(COL_LGT), points.getDouble(COL_TIME));

			while (!points.isAfterLast()) {
                int lat = points.getInt(COL_LAT);
                int lgt = points.getInt(COL_LGT);
                double time = points.getDouble(COL_TIME);

                addPointToSavedMap(lat, lgt, time);
				points.moveToNext();
			}
			points.close();
			mDb.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		gpspoints.repopulate();

		return gpspoints;
	}

	private void addPointToSavedMap(int lat, int lgt, double currentTime) {
		CyclePoint pt = new CyclePoint(lat, lgt, currentTime);

		OverlayItem opoint = new OverlayItem(pt, null, null);
		gpspoints.addOverlay(opoint);
	}

	boolean addPointNow(Location loc, double currentTime, float dst) {
		int lat = (int) (loc.getLatitude() * 1E6);
		int lgt = (int) (loc.getLongitude() * 1E6);

		// Skip duplicates
		if (latestlat == lat && latestlgt == lgt)
			return true;

		float accuracy = loc.getAccuracy();
		double altitude = loc.getAltitude();
		float speed = loc.getSpeed();

		CyclePoint pt = new CyclePoint(lat, lgt, currentTime, accuracy,
				altitude, speed);

        numpoints++;
        endTime = currentTime - this.totalPauseTime;
        distance = dst;

		latlow = Math.min(latlow, lat);
		lathigh = Math.max(lathigh, lat);
		lgtlow = Math.min(lgtlow, lgt);
		lgthigh = Math.max(lgthigh, lgt);

		latestlat = lat;
		latestlgt = lgt;

        mDb.open();
        boolean rtn = mDb.addCoordToTrip(tripid, pt);
        rtn = rtn && mDb.updateTrip(tripid, "", startTime, "", "", "",
                lathigh, latlow, lgthigh, lgtlow, distance);
        mDb.close();

        return rtn;
	}

	public boolean updateTripStatus(int tripStatus) {
		boolean rtn;
		mDb.open();
		rtn = mDb.updateTripStatus(tripid, tripStatus);
		mDb.close();
		return rtn;
	}

	public boolean getStatus(int tripStatus) {
		boolean rtn;
		mDb.open();
		rtn = mDb.updateTripStatus(tripid, tripStatus);
		mDb.close();
		return rtn;
	}

	public void updateTrip() { updateTrip("","","",""); }
	public void updateTrip(String purpose, String fancyStart, String fancyInfo, String notes) {
		// Save the trip details to the phone database. W00t!
		mDb.open();
		mDb.updateTrip(tripid, purpose,	startTime, fancyStart, fancyInfo, notes,
				lathigh, latlow, lgthigh, lgtlow, distance);
		mDb.close();
	}
}
