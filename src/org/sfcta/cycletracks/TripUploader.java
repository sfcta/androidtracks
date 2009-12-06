package org.sfcta.cycletracks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.provider.Settings.System;
import android.util.Log;

public class TripUploader {
    private final Context mCtx;

    public TripUploader(Context ctx) {
        this.mCtx = ctx;
    }

    private JSONArray getCoordsJSON(long tripId) throws JSONException {
        DbAdapter mDbHelper = new DbAdapter(this.mCtx);
        Cursor tripCoordsCursor = mDbHelper.fetchAllCoordsForTrip(tripId);
        Map<String, String> fieldMap = new HashMap<String, String>();
        fieldMap.put("rec", DbAdapter.K_POINT_TIME);
        fieldMap.put("lat", DbAdapter.K_POINT_LAT);
        fieldMap.put("lon", DbAdapter.K_POINT_LGT);
        fieldMap.put("alt", DbAdapter.K_POINT_ALT);
        fieldMap.put("spd", DbAdapter.K_POINT_SPEED);
        fieldMap.put("hac", DbAdapter.K_POINT_ACC);
        fieldMap.put("vac", DbAdapter.K_POINT_ACC);
        JSONArray tripCoords = new JSONArray();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        while (!tripCoordsCursor.isLast()) {
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
            tripCoords.put(coord);
            tripCoordsCursor.moveToNext();
        }
        return tripCoords;
    }

    private JSONObject getUserJSON() throws JSONException {
        JSONObject user = new JSONObject();
        user.put("age", "Blah");
        user.put("email", "Blah");
        user.put("gender", "Blah");
        user.put("homeZIP", "Blah");
        user.put("workZIP", "Blah");
        user.put("schoolZIP", "Blah");
        user.put("cyclingFreq", "Blah");

        return user;
    }

    private Vector<String> getTripData(long tripId) {
        Vector<String> tripData = new Vector<String>();
        DbAdapter mDbHelper = new DbAdapter(this.mCtx);
        Cursor tripCursor = mDbHelper.fetchTrip(tripId);

        String note = tripCursor.getString(tripCursor
                .getColumnIndex(DbAdapter.K_TRIP_NOTE));
        String purpose = tripCursor.getString(tripCursor
                .getColumnIndex(DbAdapter.K_TRIP_PURP));
        Double startTime = tripCursor.getDouble(tripCursor
                .getColumnIndex(DbAdapter.K_TRIP_START));

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
            final String emulatorId = "android-RunningAsEmulatorTestingDeleteMe";
            return emulatorId;
        }
        String deviceId = androidBase.concat(androidId);
        return deviceId;
    }

    private List<NameValuePair> getPostData(long tripId) throws JSONException {
        JSONArray coords = getCoordsJSON(tripId);
        JSONObject user = getUserJSON();
        String deviceId = getDeviceId();
        Vector<String> tripData = getTripData(tripId);
        String note = tripData.get(0);
        String purpose = tripData.get(1);
        String startTime = tripData.get(2);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("coords", coords.toString()));
        nameValuePairs.add(new BasicNameValuePair("user", user.toString()));
        nameValuePairs.add(new BasicNameValuePair("device", deviceId));
        nameValuePairs.add(new BasicNameValuePair("note", note));
        nameValuePairs.add(new BasicNameValuePair("purpose", purpose));
        nameValuePairs.add(new BasicNameValuePair("start", startTime));
        nameValuePairs.add(new BasicNameValuePair("version", "2"));

        return nameValuePairs;
    }

    /**
     * @param tripId
     * @return boolean Whether the post succeeded.
     * @throws JSONException
     */
    public boolean uploadTrip(long tripId) throws JSONException {
        List<NameValuePair> nameValuePairs = getPostData(tripId);

        HttpClient client = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(
                "http://bikedatabase.sfcta.org/test/post/");

        HttpResponse response;

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.v("PostData", postRequest.toString());
            response = client.execute(postRequest);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        Log.v("httpResponse", response.toString());
        return true;
    }
}
