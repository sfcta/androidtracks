package org.sfcta.cycletracks;

import java.util.Map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

public class UserInfoActivity extends Activity {
    public final static String PREF_AGE = "age";
    public final static String PREF_ZIPHOME = "ziphome";
    public final static String PREF_ZIPWORK = "zipwork";
    public final static String PREF_ZIPSCHOOL = "zipschool";
    public final static String PREF_EMAIL = "email";
    public final static String PREF_GENDER = "gender";
    public final static String PREF_CYCLEFREQ = "cyclefreq";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        Map <String, ?> prefs = settings.getAll();
        ((EditText)findViewById(R.id.TextAge)).setText((CharSequence)prefs.get("age"));
        ((EditText)findViewById(R.id.TextZipHome)).setText((CharSequence)prefs.get("ziphome"));
        ((EditText)findViewById(R.id.TextZipWork)).setText((CharSequence)prefs.get("zipwork"));
        ((EditText)findViewById(R.id.TextZipSchool)).setText((CharSequence)prefs.get("zipschool"));
        ((EditText)findViewById(R.id.TextEmail)).setText((CharSequence)prefs.get("email"));
        ((SeekBar) findViewById(R.id.SeekCycleFreq)).setProgress(Integer.parseInt((String) prefs.get("cyclefreq")));

        if (prefs.get("gender")=="M") {
            ((RadioButton) findViewById(R.id.ButtonMale)).setChecked(true);
        } else if (prefs.get("gender")=="F") {
            ((RadioButton) findViewById(R.id.ButtonFemale)).setChecked(true);
        }

        setContentView(R.layout.userprefs);
    }

    @Override
    public void onDestroy() {
        // Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("age",((EditText)findViewById(R.id.TextAge)).getText().toString());
        editor.putString("ziphome",((EditText)findViewById(R.id.TextZipHome)).getText().toString());
        editor.putString("zipwork",((EditText)findViewById(R.id.TextZipWork)).getText().toString());
        editor.putString("zipschool",((EditText)findViewById(R.id.TextZipSchool)).getText().toString());
        editor.putString("email",((EditText)findViewById(R.id.TextEmail)).getText().toString());
        editor.putString("cyclefreq",""+((SeekBar)findViewById(R.id.SeekCycleFreq)).getProgress());
        RadioGroup rbg = (RadioGroup) findViewById(R.id.RadioGroup01);
        if (rbg.getCheckedRadioButtonId() == R.id.ButtonMale) editor.putString("gender","M");
        if (rbg.getCheckedRadioButtonId() == R.id.ButtonFemale) editor.putString("gender","F");

        // Don't forget to commit your edits!!!
        editor.commit();
        super.onDestroy();
    }
}
