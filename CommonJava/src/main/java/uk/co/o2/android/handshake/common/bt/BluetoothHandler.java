package uk.co.o2.android.handshake.common.bt;

import android.os.Handler;
import android.os.Message;

import uk.co.o2.android.handshake.common.utils.Constants;
import uk.co.o2.android.handshake.common.utils.Logger;
import uk.co.o2.android.handshake.common.utils.Utils;

/**
 * Created by hostova1 on 10/07/2014.
 */
public class BluetoothHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.BluetoothMessages.MESSAGE_STATE_CHANGE:
                Logger.i(this, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                    case BluetoothService.STATE_CONNECTED:
                        break;
                    case BluetoothService.STATE_CONNECTING:
                        break;
                    case BluetoothService.STATE_LISTEN:
                    case BluetoothService.STATE_NONE:
                        break;
                }
                break;
            case Constants.BluetoothMessages.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                Logger.d(this,"Write message is: " + writeMessage);


                break;
            case Constants.BluetoothMessages.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Logger.d(this,"Read message is: " + readMessage);
                break;
            case Constants.BluetoothMessages.MESSAGE_DEVICE_NAME:
                break;
            case Constants.BluetoothMessages.MESSAGE_TOAST:
                break;
        }
    }
}
