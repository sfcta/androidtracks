/**	 CycleTracks, (c) 2009 San Francisco County Transportation Authority
 * 					  San Francisco, CA, USA
 *
 *   Licensed under the GNU GPL version 3.0.
 *   See http://www.gnu.org/licenses/gpl-3.0.txt for a copy of GPL version 3.0.
 *
 * 	 @author Billy Charlton <billy.charlton@sfcta.org>
 *
 */
package co.openbike.cycletracks;

import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class UserInfoActivity extends Activity {
    public final static int PREF_AGE = 1;
    public final static int PREF_ZIPHOME = 2;
    public final static int PREF_ZIPWORK = 3;
    public final static int PREF_ZIPSCHOOL = 4;
    public final static int PREF_EMAIL = 5;
    public final static int PREF_GENDER = 6;
    public final static int PREF_CYCLEFREQ = 7;

    private final static int MENU_SAVE = 0;

    final String[] freqDesc = {"Less than once a month", "Several times a month", "Several times per week", "Daily"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userprefs);

        // Don't pop up the soft keyboard until user clicks!
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        SeekBar sb = (SeekBar) findViewById(R.id.SeekCycleFreq);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                TextView tv = (TextView) findViewById(R.id.TextFreq);
                tv.setText(freqDesc[arg1/100]);
            }
        });

        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        Map <String, ?> prefs = settings.getAll();
        for (Entry <String, ?> p : prefs.entrySet()) {
            int key = Integer.parseInt(p.getKey());
            CharSequence value = (CharSequence) p.getValue();

            switch (key) {
            case PREF_AGE:
                ((EditText)findViewById(R.id.TextAge)).setText(value);
                break;
            case PREF_ZIPHOME:
                ((EditText)findViewById(R.id.TextZipHome)).setText(value);
                break;
            case PREF_ZIPWORK:
                ((EditText)findViewById(R.id.TextZipWork)).setText(value);
                break;
            case PREF_ZIPSCHOOL:
                ((EditText)findViewById(R.id.TextZipSchool)).setText(value);
                break;
            case PREF_EMAIL:
                ((EditText)findViewById(R.id.TextEmail)).setText(value);
                break;
            case PREF_CYCLEFREQ:
                ((SeekBar) findViewById(R.id.SeekCycleFreq)).setProgress(Integer.parseInt((String) value));
                break;
            case PREF_GENDER:
                if (value.equals("M")) {
                    ((RadioButton) findViewById(R.id.ButtonMale)).setChecked(true);
                } else if (value.equals("F")) {
                    ((RadioButton) findViewById(R.id.ButtonFemale)).setChecked(true);
                }
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        savePreferences();
        super.onDestroy();
    }

    private void savePreferences() {
        // Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(""+PREF_AGE,((EditText)findViewById(R.id.TextAge)).getText().toString());
        editor.putString(""+PREF_ZIPHOME,((EditText)findViewById(R.id.TextZipHome)).getText().toString());
        editor.putString(""+PREF_ZIPWORK,((EditText)findViewById(R.id.TextZipWork)).getText().toString());
        editor.putString(""+PREF_ZIPSCHOOL,((EditText)findViewById(R.id.TextZipSchool)).getText().toString());
        editor.putString(""+PREF_EMAIL,((EditText)findViewById(R.id.TextEmail)).getText().toString());
        editor.putString(""+PREF_CYCLEFREQ,""+((SeekBar)findViewById(R.id.SeekCycleFreq)).getProgress());
        RadioGroup rbg = (RadioGroup) findViewById(R.id.RadioGroup01);
        if (rbg.getCheckedRadioButtonId() == R.id.ButtonMale) editor.putString(""+PREF_GENDER,"M");
        if (rbg.getCheckedRadioButtonId() == R.id.ButtonFemale) editor.putString(""+PREF_GENDER,"F");

        // Don't forget to commit your edits!!!
        editor.commit();
		Toast.makeText(getBaseContext(),"User preferences saved.", Toast.LENGTH_SHORT).show();
    }

    /* Creates the menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SAVE, 0, "Save").setIcon(android.R.drawable.ic_menu_save);
        return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SAVE:
            savePreferences();
            this.finish();
            return true;
        }
        return false;
    }
}
