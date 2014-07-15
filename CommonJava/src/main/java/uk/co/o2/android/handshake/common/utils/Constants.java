package uk.co.o2.android.handshake.common.utils;

import java.util.UUID;

/**
 * Created by hostova1 on 10/07/2014.
 */
public final class Constants {
    public static final UUID MY_UUID_INSECURE = UUID.fromString("6fa7fea4-5351-4bb3-aecb-882380fee7f7");

    public static final class BluetoothMessages {
        public static final int MESSAGE_STATE_CHANGE = 0;
        public static final int MESSAGE_DEVICE_NAME = 1;
        public static final int MESSAGE_TOAST = 2;
        public static final int MESSAGE_READ = 3;
        public static final int MESSAGE_WRITE = 4;
    }

    public static final class Action {
        private static final String ACTION_BASE = "uk.co.o2.android.handshake.";
        public static final String DELETED = ACTION_BASE + "DELETED";

    }

    public static final class Extras {
        public static final String DEVICE_NAME = "device_name";
        public static final String TOAST = "toast";
        public static final String RUNNING = "running";
        public static final String CONTACT = "contact";
        public static final String BT_ADDRESS = "bt_address";
    }


    public static final class SharedPreferences {
        public static final String FIRST_NAME = "first_name";
        public static final String FAMILY_NAME = "family_name";
        public static final String PHONE_NUMBER = "phone_number";
        public static final String EMAIL_ADDRESS = "email_address";
        public static final String BT_ADDRESS = "bt_address";

    }


    public static final class Path {
        public static final String Contact = "/uk/co/o2/android/handshake/contact";
        public static final String BT_ADDRESS = "/uk/co/o2/android/handshake/bluetoothAddress";

    }
}
