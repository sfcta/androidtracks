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
