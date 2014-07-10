package uk.co.o2.android.handshake.common;

import android.app.Application;
import android.os.StrictMode;

/**
 * Created by hostova1 on 10/07/2014.
 */
public abstract class HandshakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    protected void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

    }
}
