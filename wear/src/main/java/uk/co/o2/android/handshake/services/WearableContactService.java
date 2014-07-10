
package uk.co.o2.android.handshake.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import uk.co.o2.android.handshake.MyWatchActivity;
import uk.co.o2.android.handshake.R;
import uk.co.o2.android.handshake.WearHandshakeApplication;
import uk.co.o2.android.handshake.common.bt.BluetoothHandler;
import uk.co.o2.android.handshake.common.bt.BluetoothService;
import uk.co.o2.android.handshake.common.model.Contact;
import uk.co.o2.android.handshake.common.utils.Constants;
import uk.co.o2.android.handshake.common.utils.Logger;
import uk.co.o2.android.handshake.common.utils.Utils;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class WearableContactService extends Service implements SensorEventListener {

    private Contact contact = new Contact("Hooman", "Ostovari", "07530114368", "hooman.ostovari@telefonica.com");
    private final static short MIN_POWER_LIMIT = -60;
    private final static short MAX_POWER_LIMIT = -40;
    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;
    private short prevRSSI = MIN_POWER_LIMIT;
    private BluetoothDevice mBluetoothDevice;
    private Handler mHandler = new BluetoothHandler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.BluetoothMessages.MESSAGE_STATE_CHANGE) {
                if (msg.arg1 == BluetoothService.STATE_CONNECTED) {
                    if (mBluetoothService != null) {
                        String json = WearHandshakeApplication.getGson().toJson(contact);
                        Logger.d(this,"json: " + json);
                        mBluetoothService.write(json.getBytes());
                    }
                } else if (msg.arg1 == BluetoothService.STATE_NONE || msg.arg1 == BluetoothService.STATE_LISTEN) {
                    if (mSensorManager != null && mAccelerometer != null) {
                        mSensorManager.registerListener(WearableContactService.this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
            } else if (msg.what == Constants.BluetoothMessages.MESSAGE_WRITE) {
                Utils.vibrate(WearableContactService.this);
                this.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothService.stop();
                        if (mSensorManager != null && mAccelerometer != null) {
                            mSensorManager.registerListener(WearableContactService.this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                        }
                    }
                }, 500);
            }
            super.handleMessage(msg);
        }
    };
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float gravity = 0.0f;
    private float linear_acceleration = 0.0f;
    private int counter = 0;
    private long lastTimeStamp = 0;
    private final static int HANDSHAKE_LIMIT = 3;
    private final static long TIME_LIMIT = 1000;
    private SharedPreferences sharedPreferences;

    private Runnable connectBtRunnable = new Runnable() {
        @Override
        public void run() {
            if (mBluetoothDevice != null) {
                mBluetoothAdapter.cancelDiscovery();
                startAConnectionTo(mBluetoothDevice);
            }
        }
    };

    private BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                if (rssi <= MAX_POWER_LIMIT && rssi >= MIN_POWER_LIMIT) {
                    if (rssi > prevRSSI) {
                        prevRSSI = rssi;
                        mBluetoothDevice = bluetoothDevice;
                    }
                    Logger.d(this, "Device address: " + bluetoothDevice.getAddress());
                    Logger.d(this, String.format("rssi: %d \n", rssi));
                }
                mHandler.removeCallbacks(connectBtRunnable);
                mHandler.postDelayed(connectBtRunnable, 1000);
                //Stop Handshake detection until the discovery is finished
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mBluetoothDevice != null) {
                    startAConnectionTo(mBluetoothDevice);
                }
                if (mBluetoothService != null) {
                    int state = mBluetoothService.getState();
                    if (state == BluetoothService.STATE_NONE) {
                        if (mSensorManager != null && mAccelerometer != null) {
                            mSensorManager.registerListener(WearableContactService.this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                        }

                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                prevRSSI = MIN_POWER_LIMIT;
                mBluetoothDevice = null;
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (Constants.Action.DELETED.equals(action)) {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(this, "onCreate");
        if (initialize()) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            stopSelf();
            return;
        }
        registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        setRunning(true);
        startForeground(1, createNotification());
    }


    @Override
    public void onDestroy() {
        Logger.d(this, "onDestroy");
        setRunning(false);
        super.onDestroy();
        scanBTDevices(false);
        unregisterReceiver(btReceiver);
        if (mBluetoothService != null) {
            mBluetoothService.setHandler(null);
            mBluetoothService.stop();
            mBluetoothService = null;
        }
        if (mAccelerometer != null) {
            mSensorManager.unregisterListener(this);
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void scanBTDevices(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.startDiscovery();
        } else {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void startAConnectionTo(BluetoothDevice bluetoothDevice) {
        int state = mBluetoothService.getState();
        if (state != BluetoothService.STATE_CONNECTED &&
                state != BluetoothService.STATE_CONNECTING) {
            mBluetoothService.connect(bluetoothDevice);
        }
    }

    public boolean initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Logger.e(this, "Unable to initialize BluetoothManager.");
                return false;
            }
            mBluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (mBluetoothAdapter == null) {
            Logger.e(this, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        mBluetoothService = new BluetoothService(this, mHandler);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometer == null) {
            Logger.e(this, "Unable to obtain an Accelerometer.");
            return false;
        }
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float alpha = 0.8f;
            long now = System.currentTimeMillis();
            float newLinearAcceleration;
            gravity = alpha * gravity + (1 - alpha) * event.values[1];
            newLinearAcceleration = event.values[1] - gravity;
            if (newLinearAcceleration * linear_acceleration < -1 && now - lastTimeStamp < TIME_LIMIT) {
                counter++;
            } else {
                counter = 0;
            }
            lastTimeStamp = now;

            linear_acceleration = newLinearAcceleration;
            if (counter >= HANDSHAKE_LIMIT) {
                handShakeDetected();
                counter = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void handShakeDetected() {
        Log.d("Test", "Handshake detected");
        Utils.vibrate(this);
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(WearableContactService.this);
        }
        scanBTDevices(true);
    }

    private void setRunning(boolean running) {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        }
        sharedPreferences.edit().putBoolean(Constants.Extras.RUNNING, running).apply();
    }


    private Notification createNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MyWatchActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Action action =
                new Notification.Action(R.drawable.ic_hs_notification, getString(R.string.open), pendingIntent);
        Intent intent = new Intent(Constants.Action.DELETED);
        pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_hs_notification)
                .setDeleteIntent(pendingIntent)
                .setContentTitle(getString(R.string.app_name))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.handshake))
                .addAction(action)
                .build();
    }
}