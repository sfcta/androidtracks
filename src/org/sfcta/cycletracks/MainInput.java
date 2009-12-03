package org.sfcta.cycletracks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainInput extends Activity {
	ArrayList savedtrips = new ArrayList<HashMap>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		populateList();

		final Button startButton = (Button) findViewById(R.id.ButtonStart);
		final Intent i = new Intent(this, RecordingActivity.class);
		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(i);
				MainInput.this.finish();
			}
		});
	}

	void populateList() {
		ListView listSavedTrips = (ListView) findViewById(R.id.ListSavedTrips);
		FakeAdapter fa = new FakeAdapter(this, savedtrips,
				R.layout.twolinelist, new String[] { "line1", "line2" },
				new int[] { R.id.TextView01, R.id.TextView02 });
		for (int i = 0; i < 8; i++) {
			HashMap map = new HashMap();
			map.put("line1", "Trip " + i);
			map.put("line2", "This is fun " + i);
			savedtrips.add(map);
		}
		listSavedTrips.setAdapter(fa);
	}
}

class FakeAdapter extends SimpleAdapter {

	public FakeAdapter(Context context, List<? extends Map<String, ?>> data,
			int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}

}
