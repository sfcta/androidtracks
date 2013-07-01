/**	 CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 *                                    San Francisco, CA, USA
 *
 * 	 @author Billy Charlton <billy.charlton@sfcta.org>
 *
 *   This file is part of CycleTracks.
 *
 *   CycleTracks is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   CycleTracks is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with CycleTracks.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.openbike.cycletracks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple database access helper class. Defines the basic CRUD operations, and
 * gives the ability to list all trips as well as retrieve or modify a specific
 * trip.
 *
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 *
 * **This code borrows heavily from Google demo app "Notepad" in the Android
 * SDK**
 */
public class DbAdapter {
    public static final int DATABASE_VERSION = 2;

    public static final String K_TRIP_ROWID = "_id";
    public static final String K_TRIP_PURP = "purp";
    public static final String K_TRIP_START = "start";
    public static final String K_TRIP_END = "endtime";
    public static final String K_TRIP_FANCYSTART = "fancystart";
    public static final String K_TRIP_FANCYINFO = "fancyinfo";
    public static final String K_TRIP_SAFETY = "safety";
    public static final String K_TRIP_CONVENIENCE = "convenience";
    public static final String K_TRIP_EASE = "ease";
    public static final String K_TRIP_NOTE = "note";
    public static final String K_TRIP_DISTANCE = "distance";
    public static final String K_TRIP_LATHI = "lathi";
    public static final String K_TRIP_LATLO = "latlo";
    public static final String K_TRIP_LGTHI = "lgthi";
    public static final String K_TRIP_LGTLO = "lgtlo";
    public static final String K_TRIP_STATUS = "status";

    public static final String K_POINT_ROWID = "_id";
    public static final String K_POINT_TRIP  = "trip";
    public static final String K_POINT_TIME  = "time";
    public static final String K_POINT_LAT   = "lat";
    public static final String K_POINT_LGT   = "lgt";
    public static final String K_POINT_ACC   = "acc";
    public static final String K_POINT_ALT   = "alt";
    public static final String K_POINT_SPEED = "speed";

    private static final String TAG = "DbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String TABLE_CREATE_TRIPS = "create table trips "
    	+ "(_id integer primary key autoincrement, purp text, start double, endtime double, "
    	+ "fancystart text, fancyinfo text, distance float, safety float, convenience float, ease float, note text,"
        + "lathi integer, latlo integer, lgthi integer, lgtlo integer, status integer);";

    private static final String TABLE_CREATE_COORDS = "create table coords "
    	+ "(_id integer primary key autoincrement, "
        + "trip integer, lat int, lgt int, "
        + "time double, acc float, alt double, speed float);";

    private static final String DATABASE_NAME = "data";
    private static final String DATA_TABLE_TRIPS = "trips";
    private static final String DATA_TABLE_COORDS = "coords";

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(TABLE_CREATE_TRIPS);
            db.execSQL(TABLE_CREATE_COORDS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_TRIPS);
            db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_COORDS);
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx
     *            the Context within which to work
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new instance
     * of the database. If it cannot be created, throw an exception to signal
     * the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException
     *             if the database could be neither opened or created
     */
    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public DbAdapter openReadOnly() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getReadableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    // #### Coordinate table methods ####

    public boolean addCoordToTrip(long tripid, CyclePoint pt) {
    	boolean success = true;

    	// Add the latest point
        ContentValues rowValues = new ContentValues();
        rowValues.put(K_POINT_TRIP, tripid);
        rowValues.put(K_POINT_LAT, pt.getLatitudeE6());
        rowValues.put(K_POINT_LGT, pt.getLongitudeE6());
        rowValues.put(K_POINT_TIME, pt.time);
        rowValues.put(K_POINT_ACC, pt.accuracy);
        rowValues.put(K_POINT_ALT, pt.altitude);
        rowValues.put(K_POINT_SPEED, pt.speed);

        success = success && (mDb.insert(DATA_TABLE_COORDS, null, rowValues) > 0);

        // And update the trip stats
        rowValues = new ContentValues();
        rowValues.put(K_TRIP_END, pt.time);

        success = success && (mDb.update(DATA_TABLE_TRIPS, rowValues, K_TRIP_ROWID + "=" + tripid, null) > 0);

        return success;
    }

    public boolean deleteAllCoordsForTrip(long tripid) {
        return mDb.delete(DATA_TABLE_COORDS, K_POINT_TRIP + "=" + tripid, null) > 0;
    }

    public Cursor fetchAllCoordsForTrip(long tripid) {
    	try {
            Cursor mCursor = mDb.query(true, DATA_TABLE_COORDS, new String[] {
                    K_POINT_LAT, K_POINT_LGT, K_POINT_TIME,
                    K_POINT_ACC, K_POINT_ALT, K_POINT_SPEED },
                    K_POINT_TRIP + "=" + tripid,
                    null, null, null, K_POINT_TIME, null);

            if (mCursor != null) {
                mCursor.moveToFirst();
            }
            return mCursor;
    	} catch (Exception e) {
    		Log.v("GOT!",e.toString());
    		return null;
    	}
    }

    // #### Trip table methods ####

    /**
     * Create a new trip using the data provided. If the trip is successfully
     * created return the new rowId for that trip, otherwise return a -1 to
     * indicate failure.
     */
    public long createTrip(String purp, double starttime, String fancystart,
            String note) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(K_TRIP_PURP, purp);
        initialValues.put(K_TRIP_START, starttime);
        initialValues.put(K_TRIP_FANCYSTART, fancystart);
        initialValues.put(K_TRIP_NOTE, note);
        initialValues.put(K_TRIP_STATUS, TripData.STATUS_INCOMPLETE);

        return mDb.insert(DATA_TABLE_TRIPS, null, initialValues);
    }

    public long createTrip() {
        return createTrip("", System.currentTimeMillis(), "", "");
    }

    /**
     * Delete the trip with the given rowId
     *
     * @param rowId
     *            id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteTrip(long rowId) {
        return mDb.delete(DATA_TABLE_TRIPS, K_TRIP_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     *
     * @return Cursor over all trips
     */
    public Cursor fetchAllTrips() {
        Cursor c = mDb.query(DATA_TABLE_TRIPS, new String[] { K_TRIP_ROWID,
                K_TRIP_PURP, K_TRIP_START, K_TRIP_FANCYSTART, K_TRIP_NOTE, K_TRIP_FANCYINFO },
                null, null, null, null, "-" + K_TRIP_START);
        if (c != null && c.getCount()>0) {
        	c.moveToFirst();
        }
        return c;
    }

    public Cursor fetchUnsentTrips() {
        Cursor c = mDb.query(DATA_TABLE_TRIPS, new String[] { K_TRIP_ROWID },
                K_TRIP_STATUS + "=" + TripData.STATUS_COMPLETE,
                null, null, null, null);
        if (c != null && c.getCount()>0) {
        	c.moveToFirst();
        }
        return c;
    }

    public int cleanTables() {
    	int badTrips = 0;

        Cursor c = mDb.query(DATA_TABLE_TRIPS, new String[]
                { K_TRIP_ROWID, K_TRIP_STATUS },
                K_TRIP_STATUS + "=" + TripData.STATUS_INCOMPLETE,
                null, null, null, null);

        if (c != null && c.getCount()>0) {
        	c.moveToFirst();
        	badTrips = c.getCount();

            while (!c.isAfterLast()) {
                long tripid = c.getInt(0);
                deleteAllCoordsForTrip(tripid);
                c.moveToNext();
            }
        }
        c.close();
        if (badTrips>0) {
        	mDb.delete(DATA_TABLE_TRIPS, K_TRIP_STATUS + "=" + TripData.STATUS_INCOMPLETE, null);
        }
        return badTrips;
    }

    /**
     * Return a Cursor positioned at the trip that matches the given rowId
     *
     * @param rowId id of trip to retrieve
     * @return Cursor positioned to matching trip, if found
     * @throws SQLException if trip could not be found/retrieved
     */
    public Cursor fetchTrip(long rowId) throws SQLException {
        Cursor mCursor = mDb.query(true, DATA_TABLE_TRIPS, new String[] {
                K_TRIP_ROWID, K_TRIP_PURP, K_TRIP_START, K_TRIP_FANCYSTART,
                K_TRIP_SAFETY, K_TRIP_CONVENIENCE, K_TRIP_EASE,
                K_TRIP_NOTE, K_TRIP_LATHI, K_TRIP_LATLO, K_TRIP_LGTHI,
                K_TRIP_LGTLO, K_TRIP_STATUS, K_TRIP_END, K_TRIP_FANCYINFO, K_TRIP_DISTANCE },

                K_TRIP_ROWID + "=" + rowId,

                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateTrip(long tripid, String purp, double starttime,
            String fancystart, String fancyinfo, float safety, float convenience, float ease, String note, int lathigh, int latlow,
            int lgthigh, int lgtlow, float distance) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(K_TRIP_PURP, purp);
        initialValues.put(K_TRIP_START, starttime);
        initialValues.put(K_TRIP_FANCYSTART, fancystart);
        initialValues.put(K_TRIP_SAFETY, safety);
        initialValues.put(K_TRIP_CONVENIENCE, convenience);
        initialValues.put(K_TRIP_EASE, ease);
        initialValues.put(K_TRIP_NOTE, note);
        initialValues.put(K_TRIP_LATHI, lathigh);
        initialValues.put(K_TRIP_LATLO, latlow);
        initialValues.put(K_TRIP_LGTHI, lgthigh);
        initialValues.put(K_TRIP_LGTLO, lgtlow);
        initialValues.put(K_TRIP_FANCYINFO, fancyinfo);
        initialValues.put(K_TRIP_DISTANCE, distance);

        return mDb.update(DATA_TABLE_TRIPS, initialValues, K_TRIP_ROWID + "="
                + tripid, null) > 0;
    }

    public boolean updateTripStatus(long tripid, int tripStatus) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(K_TRIP_STATUS, tripStatus);

        Log.d("MARK", "Updating trip status for trip " + tripid + " to " + tripStatus);

        return mDb.update(DATA_TABLE_TRIPS, initialValues, K_TRIP_ROWID + "="
                + tripid, null) > 0;
    }
}
