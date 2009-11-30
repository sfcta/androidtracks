package org.sfcta.cycletracks;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SaveTrip extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save);

        // Kill the GPS Service
        
        // Remove the notification
    	NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	mNotificationManager.cancelAll();
    	
		// Build the discard btn 
		final Button btnDiscard = (Button) findViewById(R.id.ButtonDiscard);
		final Intent i = new Intent(this, MainInput.class);
		btnDiscard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(getBaseContext(),"Trip discarded.", Toast.LENGTH_SHORT).show();
				startActivity(i);
				SaveTrip.this.finish();
			}
		});

		// Build the submit btn 
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		final Intent xi = new Intent(this, MainInput.class);
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(xi);
				Toast.makeText(getBaseContext(),"Trip submitted.  Thank you!", Toast.LENGTH_SHORT).show();

				SaveTrip.this.finish();
			}
		});

    }
}
