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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Toast;

public class TripUploader extends AsyncTask <Long, Integer, Boolean> {
    Context mCtx;
    DbAdapter mDb;

    public static final String TRIP_COORDS_TIME = "rec";
    public static final String TRIP_COORDS_LAT = "lat";
    public static final String TRIP_COORDS_LON = "lon";
    public static final String TRIP_COORDS_ALT = "alt";
    public static final String TRIP_COORDS_SPEED = "spd";
    public static final String TRIP_COORDS_HACCURACY = "hac";
    public static final String TRIP_COORDS_VACCURACY = "vac";

    public static final String USER_AGE = "age";
    public static final String USER_EMAIL = "email";
    public static final String USER_GENDER = "gender";
    public static final String USER_ZIP_HOME = "homeZIP";
    public static final String USER_ZIP_WORK = "workZIP";
    public static final String USER_ZIP_SCHOOL = "schoolZIP";
    public static final String USER_CYCLING_FREQUENCY = "cyclingFreq";

    public TripUploader(Context ctx) {
        super();
        this.mCtx = ctx;
        this.mDb = new DbAdapter(this.mCtx);
    }

    private JSONObject getCoordsJSON(long tripId) throws JSONException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        mDb.openReadOnly();
        Cursor tripCoordsCursor = mDb.fetchAllCoordsForTrip(tripId);

        // Build the map between JSON fieldname and phone db fieldname:
        Map<String, Integer> fieldMap = new HashMap<String, Integer>();
        fieldMap.put(TRIP_COORDS_TIME,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_TIME));
        fieldMap.put(TRIP_COORDS_LAT,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_LAT));
        fieldMap.put(TRIP_COORDS_LON,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_LGT));
        fieldMap.put(TRIP_COORDS_ALT,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ALT));
        fieldMap.put(TRIP_COORDS_SPEED,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_SPEED));
        fieldMap.put(TRIP_COORDS_HACCURACY,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ACC));
        fieldMap.put(TRIP_COORDS_VACCURACY,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ACC));

        // Build JSON objects for each coordinate:
        JSONObject tripCoords = new JSONObject();
        while (!tripCoordsCursor.isAfterLast()) {
            JSONObject coord = new JSONObject();

            coord.put(TRIP_COORDS_TIME,
            		df.format(tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_TIME))));
            coord.put(TRIP_COORDS_LAT,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_LAT)) / 1E6);
            coord.put(TRIP_COORDS_LON,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_LON)) / 1E6);
            coord.put(TRIP_COORDS_ALT,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_ALT)));
            coord.put(TRIP_COORDS_SPEED,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_SPEED)));
            coord.put(TRIP_COORDS_HACCURACY,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_HACCURACY)));
            coord.put(TRIP_COORDS_VACCURACY,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_VACCURACY)));

            tripCoords.put(coord.getString("rec"), coord);
            tripCoordsCursor.moveToNext();
        }
        tripCoordsCursor.close();
        mDb.close();
        return tripCoords;
    }

    private JSONObject getUserJSON() throws JSONException {
        JSONObject user = new JSONObject();
        Map<String, Integer> fieldMap = new HashMap<String, Integer>();
        fieldMap.put(USER_AGE, new Integer(UserInfoActivity.PREF_AGE));
        fieldMap.put(USER_EMAIL, new Integer(UserInfoActivity.PREF_EMAIL));
        fieldMap.put(USER_GENDER, new Integer(UserInfoActivity.PREF_GENDER));
        fieldMap.put(USER_ZIP_HOME, new Integer(UserInfoActivity.PREF_ZIPHOME));
        fieldMap.put(USER_ZIP_WORK, new Integer(UserInfoActivity.PREF_ZIPWORK));
        fieldMap.put(USER_ZIP_SCHOOL, new Integer(UserInfoActivity.PREF_ZIPSCHOOL));

        SharedPreferences settings = this.mCtx.getSharedPreferences("PREFS", 0);
        for (Entry<String, Integer> entry : fieldMap.entrySet()) {
               user.put(entry.getKey(), settings.getString(entry.getValue().toString(), null));
        }
        user.put(USER_CYCLING_FREQUENCY, Integer.parseInt(settings.getString(""+UserInfoActivity.PREF_CYCLEFREQ, "0"))/100);
        return user;
    }

    private Vector<String> getTripData(long tripId) {
        Vector<String> tripData = new Vector<String>();
        mDb.openReadOnly();
        Cursor tripCursor = mDb.fetchTrip(tripId);

        String note = tripCursor.getString(tripCursor.getColumnIndex(DbAdapter.K_TRIP_NOTE));
        String purpose = tripCursor.getString(tripCursor.getColumnIndex(DbAdapter.K_TRIP_PURP));
        Double startTime = tripCursor.getDouble(tripCursor.getColumnIndex(DbAdapter.K_TRIP_START));
        Double endTime = tripCursor.getDouble(tripCursor.getColumnIndex(DbAdapter.K_TRIP_END));
        Float ease = tripCursor.getFloat(tripCursor.getColumnIndex(DbAdapter.K_TRIP_EASE));
        Float safety = tripCursor.getFloat(tripCursor.getColumnIndex(DbAdapter.K_TRIP_SAFETY));
        Float convenience = tripCursor.getFloat(tripCursor.getColumnIndex(DbAdapter.K_TRIP_CONVENIENCE));
        tripCursor.close();
        mDb.close();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tripData.add(note);
        tripData.add(purpose);
        tripData.add(df.format(startTime));
        tripData.add(df.format(endTime));
        tripData.add(ease.toString());
        tripData.add(safety.toString());
        tripData.add(convenience.toString());

        return tripData;
    }

    public String getDeviceId() {
        String androidId = System.getString(this.mCtx.getContentResolver(),
                System.ANDROID_ID);
        String androidBase = "androidDeviceId-";

        if (androidId == null) { // This happens when running in the Emulator
            final String emulatorId = "android-RunningAsTestingDeleteMe";
            return emulatorId;
        }
        String deviceId = androidBase.concat(androidId);
        return deviceId;
    }

    private List<NameValuePair> getPostData(long tripId) throws JSONException {
        JSONObject coords = getCoordsJSON(tripId);
        JSONObject user = getUserJSON();
        String deviceId = getDeviceId();
        Vector<String> tripData = getTripData(tripId);
        String notes = tripData.get(0);
        String purpose = tripData.get(1);
        String startTime = tripData.get(2);
        String endTime = tripData.get(3);
        String ease = tripData.get(4);
        String safety = tripData.get(5);
        String convenience = tripData.get(6);


        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("coords", coords.toString()));
        nameValuePairs.add(new BasicNameValuePair("user", user.toString()));
        nameValuePairs.add(new BasicNameValuePair("device", deviceId));
        nameValuePairs.add(new BasicNameValuePair("notes", notes));
        nameValuePairs.add(new BasicNameValuePair("purpose", purpose));
        nameValuePairs.add(new BasicNameValuePair("start", startTime));
        nameValuePairs.add(new BasicNameValuePair("end", endTime));
        nameValuePairs.add(new BasicNameValuePair("ease", ease));
        nameValuePairs.add(new BasicNameValuePair("safety", safety));
        nameValuePairs.add(new BasicNameValuePair("convenience", convenience));
        nameValuePairs.add(new BasicNameValuePair("version", "2"));

        return nameValuePairs;
    }

    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    boolean uploadOneTrip(long currentTripId) {
        boolean result = false;

        List<NameValuePair> nameValuePairs;
        try {
            nameValuePairs = getPostData(currentTripId);
        } catch (JSONException e) {
            e.printStackTrace();
            return result;
        }
        Log.v("PostData", nameValuePairs.toString());

        HttpClient client = new DefaultHttpClient();
        final String postUrl = "http://openbike.co:1337";
        HttpPost postRequest = new HttpPost(postUrl);

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = client.execute(postRequest);
            String responseString = convertStreamToString(response.getEntity().getContent());
            Log.v("httpResponse", responseString);
            JSONObject responseData = new JSONObject(responseString);
            if (responseData.getString("status").equals("success")) {
                mDb.open();
                boolean updateResult = mDb.updateTripStatus(currentTripId, TripData.STATUS_SENT);
                Log.d("MARK", "updateTripStatus db result: " + updateResult);
                mDb.close();
                result = true;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return result;
    }

    @Override
    protected Boolean doInBackground(Long... tripid) {
        // First, send the trip user asked for:
        Boolean result = uploadOneTrip(tripid[0]);

        // Then, automatically try and send previously-completed trips
        // that were not sent successfully.
        Vector <Long> unsentTrips = new Vector <Long>();

        mDb.openReadOnly();
        Cursor cur = mDb.fetchUnsentTrips();
        if (cur != null && cur.getCount()>0) {
            //pd.setMessage("Sent. You have previously unsent trips; submitting those now.");
            while (!cur.isAfterLast()) {
                unsentTrips.add(new Long(cur.getLong(0)));
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