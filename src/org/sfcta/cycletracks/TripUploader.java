package org.sfcta.cycletracks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.Settings.System;
import android.util.Log;

public class TripUploader {
    Context mCtx;
    DbAdapter mDbHelper;

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
        this.mCtx = ctx;
        this.mDbHelper = new DbAdapter(this.mCtx);
    }

    private JSONObject getCoordsJSON(long tripId) throws JSONException {
        mDbHelper.openReadOnly();
        Cursor tripCoordsCursor = mDbHelper.fetchAllCoordsForTrip(tripId);
        Map<String, String> fieldMap = new HashMap<String, String>();
        fieldMap.put(TRIP_COORDS_TIME, DbAdapter.K_POINT_TIME);
        fieldMap.put(TRIP_COORDS_LAT, DbAdapter.K_POINT_LAT);
        fieldMap.put(TRIP_COORDS_LON, DbAdapter.K_POINT_LGT);
        fieldMap.put(TRIP_COORDS_ALT, DbAdapter.K_POINT_ALT);
        fieldMap.put(TRIP_COORDS_SPEED, DbAdapter.K_POINT_SPEED);
        fieldMap.put(TRIP_COORDS_HACCURACY, DbAdapter.K_POINT_ACC);
        fieldMap.put(TRIP_COORDS_VACCURACY, DbAdapter.K_POINT_ACC);
        JSONObject tripCoords = new JSONObject();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        while (!tripCoordsCursor.isAfterLast()) {
            JSONObject coord = new JSONObject();
            for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                int dbIndex = tripCoordsCursor.getColumnIndex(entry.getValue());
                Double dbData = tripCoordsCursor.getDouble(dbIndex);
                if (entry.getValue() == DbAdapter.K_POINT_TIME) {
                    coord.put(entry.getKey(), df.format(dbData));
                } else {
                    coord.put(entry.getKey(), dbData);
                }
            }
            tripCoords.put(coord.getString("rec"), coord);
            tripCoordsCursor.moveToNext();
        }
        tripCoordsCursor.close();
        mDbHelper.close();
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
        mDbHelper.openReadOnly();
        Cursor tripCursor = mDbHelper.fetchTrip(tripId);

        String note = tripCursor.getString(tripCursor
                .getColumnIndex(DbAdapter.K_TRIP_NOTE));
        String purpose = tripCursor.getString(tripCursor
                .getColumnIndex(DbAdapter.K_TRIP_PURP));
        Double startTime = tripCursor.getDouble(tripCursor
                .getColumnIndex(DbAdapter.K_TRIP_START));
        tripCursor.close();
        mDbHelper.close();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tripData.add(note);
        tripData.add(purpose);
        tripData.add(df.format(startTime));

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

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("coords", coords.toString()));
        nameValuePairs.add(new BasicNameValuePair("user", user.toString()));
        nameValuePairs.add(new BasicNameValuePair("device", deviceId));
        nameValuePairs.add(new BasicNameValuePair("notes", notes));
        nameValuePairs.add(new BasicNameValuePair("purpose", purpose));
        nameValuePairs.add(new BasicNameValuePair("start", startTime));
        nameValuePairs.add(new BasicNameValuePair("version", "2"));

        return nameValuePairs;
    }

    /**
     * @param tripId
     */
    public void uploadTrip(long tripId) {
        CharSequence progTitle = mCtx.getText(R.string.uploadProgressTitle);
        CharSequence progMessage = mCtx.getText(R.string.uploadProgressMessage);
        ProgressDialog pd = ProgressDialog.show(this.mCtx, progTitle, progMessage, true, false);
        UploadThread uploadThread = new UploadThread(pd, tripId);
        uploadThread.start();
    }

    private class UploadThread extends Thread {
        long tripId;
        ProgressDialog pd;

        UploadThread(ProgressDialog pd, long tripId){
            this.tripId = tripId;
            this.pd = pd;
        }
        @Override
        public void run() {
            List<NameValuePair> nameValuePairs;
            try {
                nameValuePairs = getPostData(tripId);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
            Log.v("PostData", nameValuePairs.toString());

            HttpClient client = new DefaultHttpClient();
            final String postUrl = "http://bikedatabase.sfcta.org/post/";
            HttpPost postRequest = new HttpPost(postUrl);

            try {
                postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = client.execute(postRequest);
                String responseString = convertStreamToString(response.getEntity().getContent());
                Log.v("httpResponse", responseString);
                JSONObject responseData = new JSONObject(responseString);
                if (responseData.getString("status").equals("success")) {
                    mDbHelper.open();
                    mDbHelper.updateTripMarkUploaded(tripId);
                    mDbHelper.close();
                }
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            pd.dismiss();
        }
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
}
