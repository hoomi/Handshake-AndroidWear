package uk.co.o2.android.handshake;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import uk.co.o2.android.handshake.common.bt.BluetoothHandler;
import uk.co.o2.android.handshake.common.bt.BluetoothService;
import uk.co.o2.android.handshake.common.model.Contact;
import uk.co.o2.android.handshake.common.utils.Constants;
import uk.co.o2.android.handshake.common.utils.Logger;
import uk.co.o2.android.handshake.common.utils.Utils;


public class PhoneContactService extends Service {

    private BluetoothService mBluetoothService;
    private SharedPreferences sharedPreferences;
    private Handler mHandler = new BluetoothHandler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.BluetoothMessages.MESSAGE_READ) {
                Utils.vibrate(PhoneContactService.this);
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                Logger.d(this, "Read message is: " + readMessage);
                Contact contact = MobileHandshakeApplication.getGson().fromJson(readMessage, Contact.class);
                startActivity(new Intent(PhoneContactService.this, TransparentActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Constants.Extras.CONTACT, contact));
                return;
            }
            super.handleMessage(msg);
        }
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
        mBluetoothService = new BluetoothService(this, mHandler);
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
        if (mBluetoothService != null) {
            mBluetoothService.setHandler(null);
            mBluetoothService.stop();
        }
        super.onDestroy();
    }


    private void setRunning(boolean running) {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        }
        sharedPreferences.edit().putBoolean(Constants.Extras.RUNNING, running).apply();
    }
}
