package uk.co.o2.android.handshake;

import uk.co.o2.android.handshake.common.HandshakeApplication;
import uk.co.o2.android.handshake.common.utils.Logger;

/**
 * Created by hostova1 on 10/07/2014.
 */
public class WearHandshakeActivity extends HandshakeApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            enableStrictMode();
            Logger.enableLog(BuildConfig.DEBUG);
        }
    }
}
