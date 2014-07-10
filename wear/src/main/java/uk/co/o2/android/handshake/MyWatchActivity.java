package uk.co.o2.android.handshake;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Random;

import uk.co.o2.android.handshake.services.IBeaconService;

public class MyWatchActivity extends Activity implements SensorEventListener {

    private final static int HANDSHAKE_LIMIT = 3;
    private final static long TIME_LIMIT = 1000;
    private TextView mTextView;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float gravity = 0.0f;
    private float linear_acceleration = 0.0f;
    private int counter = 0;
    private long lastTimeStamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rect_activity_my_watch);
        mTextView = (TextView) findViewById(R.id.text);
//        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
//        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
//            @Override
//            public void onLayoutInflated(WatchViewStub stub) {
//                mTextView = (TextView) stub.findViewById(R.id.text);
//                mTextView.setText("Hello World");
//            }
//        });
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        startService(new Intent(this, IBeaconService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, IBeaconService.class));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float alpha = 0.8f;
            long now = System.currentTimeMillis();
            float newLinearAcceleration = 0.0f;
            gravity = alpha * gravity + (1 - alpha) * event.values[0];
            newLinearAcceleration = event.values[0] - gravity;
            if (newLinearAcceleration * linear_acceleration < -1 && now - lastTimeStamp < TIME_LIMIT) {
                counter ++;
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
        mTextView.setText("Handshake detected");
        mTextView.setTextSize(new Random().nextFloat()*50);
    }
}
