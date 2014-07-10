package uk.co.o2.android.handshake;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class BluetoothService extends Service {
    private final static String TAG = BluetoothClass.class.getSimpleName();
    private final static UUID MY_UUID = UUID.fromString("8ce234c0-200a-11e0-bd64-0800200c9a66");
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServerSocket mBluetoothServerSocket;
    private Thread bluetoothThread;

    public BluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        } else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        Utils

        startBTThread();
    }


    private void startBTThread() {
        if (bluetoothThread == null) {
            bluetoothThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String s = "";
                    try {
                        mBluetoothServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Test_BT", MY_UUID);
                        Log.d(TAG, "Socket started");
                        BluetoothSocket bluetoothSocket = mBluetoothServerSocket.accept();
                        InputStream inputStream = bluetoothSocket.getInputStream();
//                        bluetoothSocket.getOutputStream().write("From the phone".getBytes());
                        s = Utils.getStringFromInputStream(inputStream);
                        Thread.sleep(5000);
                        bluetoothSocket.close();
                        mBluetoothServerSocket.close();
                        mBluetoothServerSocket = null;
                        Log.d(TAG, "Socket stopped");
                        Log.d(TAG, "Received message: " + s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Bluetooth_Thread");
            bluetoothThread.start();
        }
    }

    private void stopBTThread() {
        if (bluetoothThread != null) {
            if (mBluetoothServerSocket != null) {
                try {
                    mBluetoothServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mBluetoothServerSocket = null;
                }
            }
            bluetoothThread = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBTThread();
    }
}
