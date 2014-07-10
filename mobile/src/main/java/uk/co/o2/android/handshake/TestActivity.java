package uk.co.o2.android.handshake;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import uk.co.o2.android.handshake.common.utils.Constants;

public class TestActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        CheckBox checkBox = (CheckBox) findViewById(R.id.handshake_CheckBox);
        checkBox.setChecked(isRunning());
        checkBox.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, PhoneContactService.class));
    }

    private boolean isRunning() {
        if (mSharedPreferences == null) {
            mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        }
        return mSharedPreferences.getBoolean(Constants.Extras.RUNNING, false);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Intent intent = new Intent(this, PhoneContactService.class);
        if (isChecked) {
            startService(intent);
        } else {
            stopService(intent);
        }
    }
}
