package org.sfcta.cycletracks;

import com.google.android.maps.GeoPoint;

class CyclePoint extends GeoPoint {
	public double time;
	public CyclePoint(int lat, int lgt, double currentTime) {
		super(lat, lgt);
		this.time = currentTime;
	}
}
