package uk.co.o2.android.handshake.common.utils;

import java.util.UUID;

/**
 * Created by hostova1 on 10/07/2014.
 */
public final class Constants {
    public static final UUID MY_UUID_SECURE = UUID.fromString("b1e49fea-3129-4534-9b94-64a2438e8deb");
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
    }
}
