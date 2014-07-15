
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
import android.os.PowerManager;
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
public class WearableContactService extends Service implements SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private Contact contact;
    private final static short MIN_POWER_LIMIT = -60;
    private final static short MAX_POWER_LIMIT = -40;
    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;
    private short prevRSSI = MIN_POWER_LIMIT;
    private BluetoothDevice mBluetoothDevice;
    private SharedPreferences mSharedPreferences;
    private String ownerBTAddress = "";
    private Handler mHandler = new BluetoothHandler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.BluetoothMessages.MESSAGE_STATE_CHANGE) {
                if (msg.arg1 == BluetoothService.STATE_CONNECTED) {
                    if (mBluetoothService != null) {
                        String json = WearHandshakeApplication.getGson().toJson(contact);
                        Logger.d(this, "json: " + json);
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
    private final static long TIME_LIMIT = 500;
    private PowerManager.WakeLock wakeLock;
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
            Logger.d(this, "Action: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bluetoothDevice == null) {
                    return;
                }
                String btAddress = bluetoothDevice.getAddress();
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                Logger.d(this, "Device address: " + bluetoothDevice.getAddress());
                if (rssi <= MAX_POWER_LIMIT && rssi >= MIN_POWER_LIMIT) {
                    if (rssi > prevRSSI && !btAddress.equals(ownerBTAddress) && !bluetoothDevice.getName().contains("MacBook")) {
                        prevRSSI = rssi;
                        mBluetoothDevice = bluetoothDevice;
                    }
                    Logger.d(this, String.format("rssi: %d \n", rssi));
                }
                mHandler.removeCallbacks(connectBtRunnable);
                if (prevRSSI > MAX_POWER_LIMIT - 5) {
                    mHandler.post(connectBtRunnable);
                } else {
                    mHandler.postDelayed(connectBtRunnable, 800);
                }
                //Stop Handshake detection until the discovery is finished
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mBluetoothDevice != null) {
                    startAConnectionTo(mBluetoothDevice);
                }
                if (mBluetoothService != null) {
                    int state = mBluetoothService.getState();
                    if (state != BluetoothService.STATE_CONNECTED) {
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
        if (intent != null) {
            String action = intent.getAction();
            if (Constants.Action.DELETED.equals(action)) {
                stopSelf();
            }
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
        mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        contact = new Contact(mSharedPreferences.getString(Constants.SharedPreferences.FIRST_NAME, ""),
                mSharedPreferences.getString(Constants.SharedPreferences.FAMILY_NAME, ""),
                mSharedPreferences.getString(Constants.SharedPreferences.PHONE_NUMBER, ""),
                mSharedPreferences.getString(Constants.SharedPreferences.EMAIL_ADDRESS, ""));
        ownerBTAddress = sharedPreferences.getString(Constants.SharedPreferences.BT_ADDRESS, "");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (wakeLock != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Handshake Lock");
            wakeLock.acquire();
        }
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
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
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
        Logger.d(this, "Tries to connect to: " + bluetoothDevice.getName());
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
            if (newLinearAcceleration * linear_acceleration < -0.5 && now - lastTimeStamp < TIME_LIMIT) {
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Constants.SharedPreferences.FAMILY_NAME.equals(key)) {
            contact.familyName = sharedPreferences.getString(Constants.SharedPreferences.FAMILY_NAME, "");
        } else if (Constants.SharedPreferences.FIRST_NAME.equals(key)) {
            contact.firstName = sharedPreferences.getString(Constants.SharedPreferences.FIRST_NAME, "");
        } else if (Constants.SharedPreferences.PHONE_NUMBER.equals(key)) {
            contact.phoneNumber = sharedPreferences.getString(Constants.SharedPreferences.PHONE_NUMBER, "");
        } else if (Constants.SharedPreferences.EMAIL_ADDRESS.equals(key)) {
            contact.emailAddress = sharedPreferences.getString(Constants.SharedPreferences.EMAIL_ADDRESS, "");
        } else if (Constants.SharedPreferences.BT_ADDRESS.equals(key)) {
            ownerBTAddress = sharedPreferences.getString(Constants.SharedPreferences.BT_ADDRESS, "");
        }
    }
}