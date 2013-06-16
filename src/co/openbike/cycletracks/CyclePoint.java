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

import com.google.android.maps.GeoPoint;

class CyclePoint extends GeoPoint {
	public float accuracy;
	public double altitude;
	public float speed;
	public double time;

    public CyclePoint(int lat, int lgt, double currentTime) {
        super(lat, lgt);
        this.time = currentTime;
    }

    public CyclePoint(int lat, int lgt, double currentTime, float accuracy) {
        super(lat, lgt);
        this.time = currentTime;
        this.accuracy = accuracy;
    }

	public CyclePoint(int lat, int lgt, double currentTime, float accuracy, double altitude, float speed) {
		super(lat, lgt);
		this.time = currentTime;
		this.accuracy = accuracy;
		this.altitude = altitude;
		this.speed = speed;
	}
}
