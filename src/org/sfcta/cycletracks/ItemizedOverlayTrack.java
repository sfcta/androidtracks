package org.sfcta.cycletracks;

// This is all from the hello-mapview tutorial

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class ItemizedOverlayTrack extends ItemizedOverlay<OverlayItem> {
	private final ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();

	public ItemizedOverlayTrack(Drawable defaultMarker) {
		super(boundCenter(defaultMarker));
	}

	@Override
	protected OverlayItem createItem(int i) {
			return overlays.get(i);
	}

	@Override
	public int size() {
		return overlays.size();
	}

	public void addOverlay(OverlayItem overlay) {
		overlays.add(overlay);
		// populate();
	}

	public void repop() {
		populate();
	}
}
