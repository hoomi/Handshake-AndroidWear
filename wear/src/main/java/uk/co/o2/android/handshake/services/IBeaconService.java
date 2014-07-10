
package uk.co.o2.android.handshake.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import uk.co.o2.android.handshake.Utils;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class IBeaconService extends Service {

    private final static String TAG = IBeaconService.class.getSimpleName();
    private final static UUID MY_UUID = UUID.fromString("8ce234c0-200a-11e0-bd64-0800200c9a66");
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private Thread connectionThread;

    private BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device address: " + bluetoothDevice.getAddress());
//                if (bluetoothDevice != null && bluetoothDevice.getAddress().equalsIgnoreCase("38:0B:40:DC:AD:5E")) {
                if (bluetoothDevice != null && bluetoothDevice.getAddress().equalsIgnoreCase("BC:CF:CC:21:46:DA")) {
                    Log.d(TAG, "Inside If");
                    startAConnectionTo(bluetoothDevice);
                }
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                ParcelUuid parcelableUUId = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
                UUID uuid = null;
                if (parcelableUUId != null) {
                    uuid = parcelableUUId.getUuid();
                    Log.d(TAG, String.format("rssi: %d \n uuid: %s", rssi, uuid.toString()));
                } else {
                    Log.d(TAG, String.format("rssi: %d \n", rssi));
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (initialize()) {
            scanLeDevice(true);
        } else {
            stopSelf();
        }
        registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    private void startAConnectionTo(final BluetoothDevice bluetoothDevice) {
        if (connectionThread == null) {
            connectionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, String.format("Connecting to....  ", bluetoothDevice.toString()));
                    mBluetoothAdapter.cancelDiscovery();
                    String s = "";
                    try {
                        mBluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                        mBluetoothSocket.connect();
                        OutputStream outputStream = mBluetoothSocket.getOutputStream();
                        if (outputStream != null) {
                            outputStream.write("Send from the watch".getBytes());
                            outputStream.flush();
                        }
                        Thread.sleep(5000);
                        mBluetoothSocket.close();
//            s = Utils.getStringFromInputStream(bluetoothSocket.getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            mBluetoothSocket.close();
                        } catch (IOException closeException) {
                        }
                        return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        mBluetoothSocket = null;
                    }
                    Log.d(TAG, "Received Text is: " + s);
                    connectionThread = null;
                }
            });
            connectionThread.start();
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);
        unregisterReceiver(btReceiver);
        if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.startDiscovery();
        } else {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    private void printUUIDs(BluetoothDevice btDevice) {
        if (btDevice == null) {
            return;
        }

        ParcelUuid[] uuids = btDevice.getUuids();
        if (uuids != null && uuids.length > 0) {
            for (ParcelUuid uuid : uuids) {
                Log.d(TAG, "UUID: " + uuid);
            }
        }
    }
}