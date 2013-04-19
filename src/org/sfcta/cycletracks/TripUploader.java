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

package org.sfcta.cycletracks;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TripUploader extends AsyncTask <Long, Integer, Boolean> {
    private static final int COORDINATES_PER_POST = 5;
    Context mCtx;
    DbAdapter mDb;

    public TripUploader(Context ctx) {
        super();
        mCtx = ctx;
        mDb = new DbAdapter(mCtx);
    }

    private List<String> getCoordinateData(long tripId, String uuid)  {

    	// build strings of coordinates
        //the_geom,time,altitude,speed,safety,convenience,ease,haccuracy,vaccuracy,trip_id

        mDb.openReadOnly();
        Cursor tripCursor = mDb.fetchTrip(tripId);

        float ease = tripCursor.getFloat(tripCursor.getColumnIndex(DbAdapter.K_TRIP_EASE));
        float safety = tripCursor.getFloat(tripCursor.getColumnIndex(DbAdapter.K_TRIP_SAFETY));
        float convenience = tripCursor.getFloat(tripCursor.getColumnIndex(DbAdapter.K_TRIP_CONVENIENCE));
        tripCursor.close();

        Cursor tripCoordsCursor = mDb.fetchAllCoordsForTrip(tripId);
        List<String> data = new ArrayList<String>(tripCoordsCursor.getCount());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    	while (!tripCoordsCursor.isAfterLast()) {
            StringBuilder coord = new StringBuilder();
            double accuracy = tripCoordsCursor.getDouble(tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ACC));
            coord.append("ST_GeomFromText('POINT(")
                    .append(tripCoordsCursor.getDouble(tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_LGT)) / 1.0E6)
                    .append(' ')
                    .append(tripCoordsCursor.getDouble(tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_LAT)) / 1.0E6)
                    .append(")', 4326),'")
                    .append(df.format(tripCoordsCursor.getDouble(tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_TIME))))
                    .append("','")
                    .append(tripCoordsCursor.getDouble(tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ALT)))
                    .append("','")
                    .append(tripCoordsCursor.getDouble(tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_SPEED)))
                    .append("','")
                    .append(safety)
                    .append("','")
                    .append(convenience)
                    .append("','")
                    .append(ease)
                    .append("','")
                    .append(accuracy)
                    .append("','")
                    .append(accuracy)
                    .append("','")
                    .append(uuid)
                    .append('\'');
            data.add(coord.toString());

            tripCoordsCursor.moveToNext();
        }
        tripCoordsCursor.close();
        mDb.close();

        return data;
    }

    private String getQuotedStringOrNull(SharedPreferences settings, int index) {
        String value = settings.getString(Integer.toString(index), null);
        if(value == null || value.trim().equals(""))
        {
            return "null";
        }
        else
        {
            return '\''+value+'\'';
        }
    }

    private String getUserData() {
        SharedPreferences settings = mCtx.getSharedPreferences("PREFS", 0);

        StringBuilder user = new StringBuilder();
        user.append(getQuotedStringOrNull(settings, UserInfoActivity.PREF_AGE)).append(',');
        user.append(getQuotedStringOrNull(settings, UserInfoActivity.PREF_EMAIL)).append(',');
        user.append(getQuotedStringOrNull(settings, UserInfoActivity.PREF_GENDER)).append(',');
        user.append(getQuotedStringOrNull(settings, UserInfoActivity.PREF_ZIPHOME)).append(',');
        user.append(getQuotedStringOrNull(settings, UserInfoActivity.PREF_ZIPWORK)).append(',');
        user.append(getQuotedStringOrNull(settings, UserInfoActivity.PREF_ZIPSCHOOL)).append(',');
        user.append('\'').append(Integer.toString(Integer.parseInt(settings.getString(Integer.toString(UserInfoActivity.PREF_CYCLEFREQ), "0"))/100)).append("',");
        user.append('\'').append(getDeviceId()).append('\'');
        return user.toString();
    }

    public String getDeviceId() {
        String androidId = System.getString(mCtx.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String androidBase = "androidDeviceId-";

        if (androidId == null) { // This happens when running in the Emulator
            return "android-RunningAsTestingDeleteMe";
        }
        return androidBase.concat(androidId);
    }

    private String getTripData(long tripId, String uuid) {
        String deviceId = getDeviceId();

        mDb.openReadOnly();
        Cursor tripCursor = mDb.fetchTrip(tripId);

        String notes = tripCursor.getString(tripCursor
                                                    .getColumnIndex(DbAdapter.K_TRIP_NOTE));
        String purpose = tripCursor.getString(tripCursor
                .getColumnIndex(DbAdapter.K_TRIP_PURP));
        Double startTime = tripCursor.getDouble(tripCursor
                                                        .getColumnIndex(DbAdapter.K_TRIP_START));
        Double endTime = tripCursor.getDouble(tripCursor
                                                      .getColumnIndex(DbAdapter.K_TRIP_END));
        tripCursor.close();
        mDb.close();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // query format device_id,trip_id,notes,purpose,start,end,version

        StringBuilder queryValues = new StringBuilder();
        queryValues.append('\'').append(deviceId).append("\',");
        queryValues.append('\'').append(uuid).append("\',");
        if(notes.trim().equals(""))
        {
            queryValues.append("null,");
        }
        else
        {
            queryValues.append('\'').append(notes).append("\',");
        }
        queryValues.append('\'').append(purpose).append("\',");
        queryValues.append('\'').append(df.format(startTime)).append("\',");
        queryValues.append('\'').append(df.format(endTime)).append("\',");
        queryValues.append("\'"+ DbAdapter.DATABASE_VERSION).append('\'');

        return queryValues.toString();
    }

    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        StringBuilder sb = new StringBuilder();

        String line = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    boolean uploadOneTrip(long currentTripId) {

        String userValues = getUserData();
        Log.v("UserData", userValues);

        String uuid = getUniqueTripId();

        String tripValues = getTripData(currentTripId, uuid);
        Log.v("TripData", tripValues);

        List<String> coordinateValues = getCoordinateData(currentTripId, uuid);

        String userQuery = "INSERT INTO users(\"age\",\"email\",\"gender\",\"zip_home\",\"zip_work\",\"zip_school\",\"cycling_frequency\",\"device_id\") VALUES(" + userValues + ')';
        String tripQuery = "INSERT INTO trips(\"device_id\",\"trip_id\",\"notes\",\"purpose\",\"start\",\"end\",\"version\") VALUES (" + tripValues + ')';

        // Break up the coordinates into groups to avoid running out of string space
        List<String> coordQueries = new LinkedList<String>();
        int i = 0;
        StringBuilder coordsQuery = new StringBuilder();
        String coordsInsert = "INSERT INTO coordinates(\"the_geom\",\"time\",\"altitude\",\"speed\",\"safety\",\"convenience\",\"ease\",\"haccuracy\",\"vaccuracy\",\"trip_id\") VALUES (";
        coordsQuery.append(coordsInsert);
        for(String coordinate : coordinateValues)
        {
            Log.v("CoordData", coordinate);
            coordsQuery.append(coordinate);
            coordsQuery.append("), (");
            if(++i >= COORDINATES_PER_POST)
            {
                coordsQuery.delete(coordsQuery.lastIndexOf(","), coordsQuery.length());
                coordQueries.add(coordsQuery.toString());
                coordsQuery = new StringBuilder();
                coordsQuery.append(coordsInsert);
            }
        }
        coordsQuery.delete(coordsQuery.lastIndexOf(","), coordsQuery.length());
        coordQueries.add(coordsQuery.toString());

        boolean success = false;
        boolean postSuccess = postQuery(userQuery);
        postSuccess = postSuccess && postQuery(tripQuery);
        for(String query : coordQueries)
        {
            postSuccess = postSuccess && postQuery(query);
        }

        if (postSuccess) {
            mDb.open();
            mDb.updateTripStatus(currentTripId, TripData.STATUS_SENT);
            mDb.close();
            success = true;
        }

        return success;
    }

    private String getUniqueTripId()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getDeviceId());
        buffer.append('-');
        buffer.append(java.lang.System.currentTimeMillis());
        return buffer.toString();
    }

    private boolean postQuery(String query)
    {
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Log.v("httpError", e.getMessage(), e);
            return false;
        }
        Log.v("postQuery", query);

        final String postUrl = "http://openbike.cartodb.com/api/v2/sql?q=" + encodedQuery + "&api_key=274e353c1814bc0308f94e82ea18ff10a1a4bb4a";
        Log.v("postUrl", postUrl);

        HttpPost postRequest = new HttpPost(postUrl);
        Log.v("postRequest", postRequest.toString());

        try {
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(postRequest);
            String responseString = convertStreamToString(response.getEntity().getContent());
            Log.v("httpResponse", responseString);
            JSONObject responseData = new JSONObject(responseString);
            return Integer.parseInt(responseData.getString("total_rows")) > 0;
        } catch (IllegalStateException e) {
            Log.v("httpError", e.getMessage(), e);
            return false;
        } catch (IOException e) {
            Log.v("httpError", e.getMessage(), e);
            return false;
        } catch (JSONException e) {
            Log.v("httpError", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected Boolean doInBackground(Long... tripid) {
        // First, send the trip user asked for:
        Boolean result = uploadOneTrip(tripid[0]);

        // Then, automatically try and send previously-completed trips
        // that were not sent successfully.
        List<Long> unsentTrips = new LinkedList<Long>();

        mDb.openReadOnly();
        Cursor cur = mDb.fetchUnsentTrips();
        if (cur != null && cur.getCount()>0) {
            //pd.setMessage("Sent. You have previously unsent trips; submitting those now.");
            while (!cur.isAfterLast()) {
                unsentTrips.add(cur.getLong(0));
                cur.moveToNext();
            }
            cur.close();
        }
        mDb.close();

        for (Long trip: unsentTrips) {
            result &= uploadOneTrip(trip);
        }
        return result;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mCtx.getApplicationContext(),"Submitting trip.  Thanks for using CycleTracks!", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        try {
            if (result) {
                Toast.makeText(mCtx.getApplicationContext(),"Trip uploaded successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mCtx.getApplicationContext(),"CycleTracks couldn't upload the trip, and will retry when your next trip is completed.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            // Just don't toast if the view has gone out of context
        }
    }
}
