package uk.co.o2.android.handshake;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;

import uk.co.o2.android.handshake.common.bt.BluetoothHandler;
import uk.co.o2.android.handshake.common.bt.BluetoothService;
import uk.co.o2.android.handshake.common.utils.Constants;


public class PhoneContactService extends Service {

    private BluetoothService mBluetoothService;
    private SharedPreferences sharedPreferences;
    private Handler mHandler  = new BluetoothHandler() {

    };
    public PhoneContactService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothService = new BluetoothService(this,mHandler);
        mBluetoothService.start();
        setRunning(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        setRunning(false);
        if (mBluetoothService !=  null) {
            mBluetoothService.setHandler(null);
            mBluetoothService.stop();
            mBluetoothService = null;
        }
        super.onDestroy();
    }


    private void setRunning(boolean running) {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        }
        sharedPreferences.edit().putBoolean(Constants.Extras.RUNNING, running).commit();
    }
}
