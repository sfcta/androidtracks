package org.sfcta.cycletracks;

import java.text.SimpleDateFormat;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;

public class TripUploader {
	private final Context mCtx;

	public TripUploader(Context ctx) {
        this.mCtx = ctx;
    }

	private JSONArray getCoordsJSON(long tripId) throws JSONException {
		DbAdapter mDbHelper = new DbAdapter(this.mCtx);
		Cursor tripCoordsCursor = mDbHelper.fetchAllCoordsForTrip(tripId);
		String[] dbFields = {DbAdapter.K_POINT_TIME, DbAdapter.K_POINT_LAT, DbAdapter.K_POINT_LGT, DbAdapter.K_POINT_ALT, DbAdapter.K_POINT_ACC, DbAdapter.K_POINT_SPEED};
		JSONArray tripCoords = new JSONArray();
		while (!tripCoordsCursor.isLast()) {
			JSONObject coord = new JSONObject();
			for (int i=0; i<dbFields.length; i++) {
		         coord.put(dbFields[i], tripCoordsCursor.getDouble(tripCoordsCursor.getColumnIndex(dbFields[i])));
			}
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			coord.put(DbAdapter.K_POINT_TIME, df.format(coord.get(DbAdapter.K_POINT_TIME)));
			tripCoords.put(coord);
			tripCoordsCursor.moveToNext();
		}
		return tripCoords;
	}

	private JSONObject getUserJSON() throws JSONException {
	    JSONObject user = new JSONObject();
        return user;
    }

	private Vector<String> getTripData(long tripId)  {
	    Vector<String> tripData = new Vector<String>();
	    DbAdapter mDbHelper = new DbAdapter(this.mCtx);

        return tripData;
    }

	public boolean uploadTrip(long tripId) throws JSONException{
	    JSONArray coords = getCoordsJSON(tripId);
	    JSONObject user = getUserJSON();
	    Vector<String> tripData = getTripData(tripId);
	    String note = tripData.get(0);
	    String purpose = tripData.get(1);
	    String startTime = tripData.get(2);



        return false;

	}

}
