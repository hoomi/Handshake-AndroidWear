package uk.co.o2.android.handshake.common.utils;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by hostova1 on 09/07/2014.
 */
public final class Utils {
    public static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(200);
        }
    }
}
