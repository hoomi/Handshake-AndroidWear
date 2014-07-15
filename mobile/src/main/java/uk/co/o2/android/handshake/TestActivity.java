package uk.co.o2.android.handshake;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

import uk.co.o2.android.handshake.common.HandshakeApplication;
import uk.co.o2.android.handshake.common.model.Contact;
import uk.co.o2.android.handshake.common.utils.Constants;
import uk.co.o2.android.handshake.common.utils.Logger;

public class TestActivity extends Activity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
    private EditText mFirstNameEditText;
    private EditText mSurnameEditText;
    private EditText mPhoneNumberEditText;
    private EditText mEmailAddressEditText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        CheckBox checkBox = (CheckBox) findViewById(R.id.handshake_CheckBox);
        checkBox.setChecked(isRunning());
        checkBox.setOnCheckedChangeListener(this);
        initGoogleApiClient();
        updateEditText();
    }

    @Override
    protected void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
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

    private void initGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this, new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    Logger.d(this, "Connected to Google Play Services");
                    syncBluetoothAddress();
                }

                @Override
                public void onConnectionSuspended(int i) {
                    Logger.d(this, "Disconnected from Google Play Services");
                    mGoogleApiClient = null;
                }
            }, new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    Logger.e(this, "Connection to Google Play services failed: " + connectionResult.getErrorCode());
                    mGoogleApiClient = null;
                }
            }).addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.save_Button:
                saveDetails();
                break;
        }
    }

    private void saveDetails() {
        Contact contact = new Contact(mFirstNameEditText.getText().toString(),
                mSurnameEditText.getText().toString(),
                mPhoneNumberEditText.getText().toString(),
                mEmailAddressEditText.getText().toString());
        syncDataWithTheWatch(contact);
    }

    public void syncDataWithTheWatch(final Contact contact) {
        if (contact == null) {
            return;
        }
        PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.Path.Contact);
        dataMap.getDataMap().putString(Constants.Extras.CONTACT, HandshakeApplication.getGson().toJson(contact));
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    sharedPreferences.edit()
                            .putString(Constants.SharedPreferences.FAMILY_NAME, contact.familyName)
                            .putString(Constants.SharedPreferences.FIRST_NAME, contact.firstName)
                            .putString(Constants.SharedPreferences.PHONE_NUMBER, contact.phoneNumber)
                            .putString(Constants.SharedPreferences.EMAIL_ADDRESS, contact.emailAddress)
                            .apply();
                }

            }
        }, 5, TimeUnit.SECONDS);
    }

    public void syncBluetoothAddress() {
        BluetoothAdapter bluetoothAdapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String bluetoothAddress = bluetoothAdapter.getAddress();
        if (!TextUtils.isEmpty(bluetoothAddress)) {
            PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.Path.BT_ADDRESS);
            dataMap.getDataMap().putString(Constants.Extras.BT_ADDRESS, bluetoothAdapter.getAddress());
            PutDataRequest request = dataMap.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                    .putDataItem(mGoogleApiClient, request);
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    if (dataItemResult.getStatus().isSuccess()) {
                        Logger.d(this,"Bluetooth address updated successfully");
                    }

                }
            }, 5, TimeUnit.SECONDS);
        }
    }


    private void updateEditText() {
        if (mFirstNameEditText == null) {
            mFirstNameEditText = (EditText) findViewById(R.id.firstName_EditText);
            mSurnameEditText = (EditText) findViewById(R.id.surname_EditText);
            mPhoneNumberEditText = (EditText) findViewById(R.id.phoneNumber_EditText);
            mEmailAddressEditText = (EditText) findViewById(R.id.emailAddress_EditText);
        }
        mFirstNameEditText.setText(mSharedPreferences.getString(Constants.SharedPreferences.FIRST_NAME, ""));
        mSurnameEditText.setText(mSharedPreferences.getString(Constants.SharedPreferences.FAMILY_NAME, ""));
        mPhoneNumberEditText.setText(mSharedPreferences.getString(Constants.SharedPreferences.PHONE_NUMBER, ""));
        mEmailAddressEditText.setText(mSharedPreferences.getString(Constants.SharedPreferences.EMAIL_ADDRESS, ""));
    }

}
