package uk.co.o2.android.handshake.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

import uk.co.o2.android.handshake.R;
import uk.co.o2.android.handshake.WearHandshakeApplication;
import uk.co.o2.android.handshake.common.model.Contact;
import uk.co.o2.android.handshake.common.utils.Constants;
import uk.co.o2.android.handshake.common.utils.Logger;

public class DataChangeListenerService extends WearableListenerService {
    public DataChangeListenerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(this, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Logger.d(this, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        Logger.d(this, "onCreate");
        super.onCreate();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Logger.e(this, "Failed to connect to GoogleApiClient.");
            return;
        }
        if (dataEvents.getStatus().isSuccess()) {
            for (DataEvent event : dataEvents) {
                Uri uri = event.getDataItem().getUri();

                // Get the node id from the host value of the URI
                String nodeId = uri.getHost();
                // Set the data of the message to be the bytes of the URI.
                byte[] payload = uri.toString().getBytes();
                String path = uri.getPath();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
                if (dataMapItem != null) {
                    if (Constants.Path.Contact.equals(path)) {
                        Contact contact = WearHandshakeApplication.getGson().fromJson(dataMapItem.getDataMap().getString(Constants.Extras.CONTACT), Contact.class);
                        Logger.d(this, "Contact: " + contact.toString());
                        sharedPreferences.edit()
                                .putString(Constants.SharedPreferences.FAMILY_NAME, contact.familyName)
                                .putString(Constants.SharedPreferences.FIRST_NAME, contact.firstName)
                                .putString(Constants.SharedPreferences.PHONE_NUMBER, contact.phoneNumber)
                                .putString(Constants.SharedPreferences.EMAIL_ADDRESS, contact.emailAddress)
                                .apply();
                        Toast.makeText(this, R.string.contact_updated, Toast.LENGTH_SHORT).show();
                    } else if (Constants.Path.BT_ADDRESS.equals(path)) {
                        String btAddress = dataMapItem.getDataMap().getString(Constants.Extras.CONTACT);
                        Logger.d(this, "BT address: " + btAddress);
                        sharedPreferences.edit()
                                .putString(Constants.SharedPreferences.BT_ADDRESS, btAddress)
                                .apply();
                    }
                }


                // Send the RPC
                Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                        Constants.Path.Contact, payload);
            }
        }
        super.onDataChanged(dataEvents);
    }
}
