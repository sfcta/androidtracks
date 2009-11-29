package org.sfcta.cycletracks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class RecordingActivity extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording);
        final Button finishButton = (Button) findViewById(R.id.ButtonFinished);
        final Intent i = new Intent(this, SaveTrip.class);
        finishButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	startActivity(i);
            }
        });

    }
}
