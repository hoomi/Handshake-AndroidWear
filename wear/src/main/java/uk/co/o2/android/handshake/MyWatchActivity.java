package uk.co.o2.android.handshake;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import uk.co.o2.android.handshake.common.utils.Constants;
import uk.co.o2.android.handshake.common.utils.Logger;
import uk.co.o2.android.handshake.services.WearableContactService;

public class MyWatchActivity extends Activity{

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Logger.d(this,"isChecked: " + isChecked);
            Intent intent = new Intent(MyWatchActivity.this, WearableContactService.class);
            if (isChecked) {
                startService(intent);
            } else {
                stopService(intent);
            }
        }
    };

    private SharedPreferences mSharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_watch);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                CheckBox checkBox = (CheckBox)stub.findViewById(R.id.handshake_CheckBox);
                checkBox.setChecked(isRunning());
                checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean isRunning() {
        if (mSharedPreferences == null) {
            mSharedPreferences = getSharedPreferences(getPackageName(),MODE_PRIVATE);
        }
        return  mSharedPreferences.getBoolean(Constants.Extras.RUNNING,false);
    }
}
