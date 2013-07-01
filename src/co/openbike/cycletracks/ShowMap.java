/**  CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 *                                    San Francisco, CA, USA
 *
 *   @author Billy Charlton <billy.charlton@sfcta.org>
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

// Tips and Tricks related to Mark's upgrade from Maps v1 to v2

//4A:12:0D:09:BD:12:6E:E9:90:63:45:E2:E4:F2:5F:24:47:EE:80:14
//SHA-1 of debug.keystore - https://developers.google.com/maps/documentation/android/start
/*
 * Key for Android apps (with certificates)
 API key:
 AIzaSyBsMS0AQ68bro0VByGEcs123SEWJ5t7zy8
 Android apps:
 4A:12:0D:09:BD:12:6E:E9:90:63:45:E2:E4:F2:5F:24:47:EE:80:14;org.sfcta.cycletracks

 https://docs.google.com/document/pub?id=19nQzvKP-CVLd7_VrpwnHfl-AE9fjbJySowONZZtNHzw
 //find location of lib by going into sdk downloads and hovering over map lib in extras
 *
 * Attribution Requirements. If you use the Google Maps Android API in your application, you must include the Google Play Services attribution text as part of a "Legal Notices" section in your application. Including legal notices as an independent menu item, or as part of an "About" menu item, is recommended. The attribution text is available by making a call to GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo.
 *
 *
 *adding supprt v4 - http://developer.android.com/tools/extras/support-library.html#SettingUp
 *
 *helpful - http://stackoverflow.com/questions/13719263/unable-instantiate-android-gms-maps-mapfragment
 *
 http://stackoverflow.com/questions/16589821/android-app-crashes-after-sdk-tools-update-version-noclassdeffound-tool-versio

 found a bug - after you record track and submit it and are on map page, an orientation change triggers a resubmission and you see toast
 */
package co.openbike.cycletracks;

import java.util.ArrayList;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class ShowMap extends FragmentActivity {
	Drawable drawable;
	ItemizedOverlayTrack gpspoints;
	GoogleMap mMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mapview);

		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();

		try {

			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0))
					.title("Marker"));
			mMap.setMyLocationEnabled(true);

			Bundle cmds = getIntent().getExtras();
			long tripid = cmds.getLong("showtrip");

			TripData trip = TripData.fetchTrip(this, tripid);

			// Show trip details
			TextView t1 = (TextView) findViewById(R.id.TextViewT1);
			TextView t2 = (TextView) findViewById(R.id.TextViewT2);
			TextView t3 = (TextView) findViewById(R.id.TextViewT3);
			t1.setText(trip.purp);
			t2.setText(trip.info);
			t3.setText(trip.fancystart);

			// Center & zoom the map
			// need to divide by 1 power 6 to get values that will play nice
			double higha = (trip.lathigh / 1E6);
			double lowa = (trip.latlow / 1E6);
			double higho = (trip.lgthigh / 1E6);
			double lowo = (trip.lgtlow / 1E6);
			double latcenter = (higha + lowa) / 2;
			double lgtcenter = (higho + lowo) / 2;
			LatLng center = new LatLng(latcenter, lgtcenter);
			// debug output,
			Log.d("MARK", "lat ctr: " + latcenter + ", lon ctr: " + lgtcenter);
			// values for zoom are 1 to 21 with 21 being way zoomed in 15 is a
			// couple block radius
			// could get fancier here and alter the zoom level by the overall
			// distance
			// but good enough for now
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15.0f));

			if (gpspoints == null) {
				AddPointsToMapLayerTask maptask = new AddPointsToMapLayerTask();
				maptask.execute(trip);
			} else {
				// mapOverlays.add(gpspoints);
			}
			Log.d("MARK", "Possibly uploading trip data, trip status is: "
					+ trip.status);
			// TODO: pass trip object into uploader and when db is updated,
			// update the property of trip too (status) to avoid extra posts
			if (trip.status < TripData.STATUS_SENT && cmds != null
					&& cmds.getBoolean("uploadTrip", false)) {
				// And upload to the cloud database, too! W00t W00t!
				TripUploader uploader = new TripUploader(ShowMap.this);
				uploader.execute(trip.tripid);
			}

		} catch (Exception e) {
			Log.e("GOT!", e.toString());
		}
	}

	// @Override
	// protected boolean isRouteDisplayed() {
	// // Auto-generated method stub
	// return false;
	// }

	// Make sure overlays get zapped when we go BACK
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mMap != null) {
			mMap.clear();
		}
		return super.onKeyDown(keyCode, event);
	}

	private class AddPointsToMapLayerTask extends
			AsyncTask<TripData, Integer, ItemizedOverlayTrack> {
		TripData trip;

		@Override
		protected ItemizedOverlayTrack doInBackground(TripData... trips) {
			trip = trips[0]; // always get just the first trip

			drawable = getResources().getDrawable(R.drawable.point);
			ShowMap.this.gpspoints = trip.getPoints(drawable);

			return ShowMap.this.gpspoints;
		}

		@Override
		protected void onPostExecute(ItemizedOverlayTrack gpspoints) {
			// Lets convert this track into an array list of lat/lng points
			//https://developers.google.com/maps/documentation/android/reference/com/google/android/gms/maps/model/Polyline
			DbAdapter mDb;
			mDb = new DbAdapter(getApplicationContext());
			mDb.openReadOnly();

			Cursor points = mDb.fetchAllCoordsForTrip(trip.tripid);
			ArrayList<LatLng> dataPoints = new ArrayList<LatLng>();
			while (points.moveToNext()) {
				double lat = points.getDouble(points
						.getColumnIndex(mDb.K_POINT_LAT)) / 1E6;
				double lng = points
						.getDouble(points.getColumnIndex(mDb.K_POINT_LGT)) / 1E6;
				dataPoints.add(new LatLng(lat, lng));
//				Log.d("MARK", "Adding: " + lat + ", " + lng);
			}
			Log.d("MARK", "points size: " + points.getCount());
			PolylineOptions polylineOptions = new PolylineOptions()
					.addAll(dataPoints).width(5).color(Color.RED);
			Polyline polyline = mMap.addPolyline(polylineOptions);

			// Add the points
			// mapOverlays.add(ShowMap.this.gpspoints);

			// Add the lines! W00t!
			// mapOverlays.add(new LineOverlay(ShowMap.this.gpspoints));

			// Add start & end pins
			if (trip.startpoint != null) {
				// mapOverlays.add(new PushPinOverlay(trip.startpoint,
				// R.drawable.pingreen));
			}
			if (trip.endpoint != null) {
				// mapOverlays.add(new PushPinOverlay(trip.endpoint,
				// R.drawable.pinpurple));
			}

			// Redraw the map
			// mapView.invalidate();
		}
	}

	private static class LineOverlay extends Overlay {
		private static final float REQ_ACCURACY_FOR_LINES = 25.0f; // Was 8.0,
																	// increased
																	// because
																	// test data
																	// was
																	// coming in
																	// at 20.0
		private final ItemizedOverlayTrack track;

		public LineOverlay(ItemizedOverlayTrack track) {
			super();
			this.track = track;
		}

		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
				long when) {
			super.draw(canvas, mapView, shadow);

			// Need at least two points to draw a line, duh
			if (track.size() < 2)
				return true;

			// Build array of points
			float[] points = new float[4 * track.size()];
			int segments = 0;
			int startx = -1;
			int starty = -1;

			for (int i = 0; i < track.size(); i++) {
				CyclePoint z = (CyclePoint) track.getItem(i).getPoint();

				// Skip lousy points
				if (z.accuracy > REQ_ACCURACY_FOR_LINES) {
					Log.v("debug", "skipping points with accuracy of "
							+ z.accuracy);
					startx = -1;
					continue;
				}

				// If this is the beginning of a new segment, great
				Point screenPoint = new Point();
				mapView.getProjection().toPixels(z, screenPoint);

				if (startx == -1) {
					startx = screenPoint.x;
					starty = screenPoint.y;
					continue;
				}
				int numpts = segments * 4;
				points[numpts] = startx;
				points[numpts + 1] = starty;
				points[numpts + 2] = startx = screenPoint.x;
				points[numpts + 3] = starty = screenPoint.y;
				segments++;
			}

			// Line style
			Paint paint = new Paint();
			paint.setARGB(255, 0, 0, 255);
			paint.setStrokeWidth(5);
			paint.setStyle(Style.FILL_AND_STROKE);

			canvas.drawLines(points, 0, segments * 4, paint);
			return false;
		}
	}

	class PushPinOverlay extends Overlay {
		GeoPoint p;
		int d;

		public PushPinOverlay(GeoPoint p, int drawrsrc) {
			super();
			this.p = p;
			this.d = drawrsrc;
		}

		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
				long when) {
			super.draw(canvas, mapView, shadow);

			// ---translate the GeoPoint to screen pixels---
			Point screenPoint = new Point();
			mapView.getProjection().toPixels(p, screenPoint);

			// ---add the marker---
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), d);
			int height = bmp.getScaledHeight(canvas);
			int width = Double.valueOf(0.133333 * bmp.getScaledWidth(canvas))
					.intValue(); // 4/30 pixels: how far right we want the
									// pushpin

			canvas.drawBitmap(bmp, screenPoint.x - width, screenPoint.y
					- height, null);
			return true;
		}
	}
}
