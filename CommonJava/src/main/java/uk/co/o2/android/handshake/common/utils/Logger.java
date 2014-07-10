package uk.co.o2.android.handshake.common.utils;

import android.util.Log;

import uk.co.o2.android.handshake.common.BuildConfig;

public final class Logger {

    private static boolean SHOW_LINE_NUMBER = false;
    private static boolean DEBUG = false;


    public static void enableLog(boolean enable) {
        DEBUG = enable;
    }
    /**
     * It shows the line number and function that the log function is called from
     */
    public static void showLineNumbers(){
        SHOW_LINE_NUMBER = true;
    }

    /**
     * It does the opposite of show line numbers
     */
    public static void hideLineNumber(){
        SHOW_LINE_NUMBER = false;
    }


    public static void i(Object object, String message) {
        if (BuildConfig.DEBUG || DEBUG) {
            String debugMessage = message;
            if (SHOW_LINE_NUMBER) {
                debugMessage = addAdditionalInformation() + message;
            }
            Log.i(object.getClass().getSimpleName(), debugMessage);
        }
    }

    public static void d(Object object, String message) {
        if (BuildConfig.DEBUG || DEBUG) {
            String debugMessage = message;
            if (SHOW_LINE_NUMBER) {
                debugMessage = addAdditionalInformation() + message;
            }
            Log.d(object.getClass().getSimpleName(), debugMessage);
        }
    }

    public static void w(Object object, String message) {
        if (BuildConfig.DEBUG || DEBUG) {
            String debugMessage = message;
            if (SHOW_LINE_NUMBER) {
                debugMessage = addAdditionalInformation() + message;
            }
            Log.w(object.getClass().getSimpleName(), debugMessage);
        }
    }

    public static void e(Object object, String message) {
        if (BuildConfig.DEBUG || DEBUG) {
            String debugMessage = message;
            if (SHOW_LINE_NUMBER) {
                debugMessage = addAdditionalInformation() + message;
            }
            Log.e(object.getClass().getSimpleName(), debugMessage);
        }
    }

    public static void e(Object object, String message,Throwable tr) {
        if (BuildConfig.DEBUG || DEBUG) {
            Log.e(object.getClass().getSimpleName(), message,tr);
        }
    }

    public static void v(Object object, String message) {
        if (BuildConfig.DEBUG || DEBUG) {
            String debugMessage = message;
            if (SHOW_LINE_NUMBER) {
                debugMessage = addAdditionalInformation() + message;
            }
            Log.v(object.getClass().getSimpleName(), debugMessage);
        }
    }

    private static String addAdditionalInformation() {
        StackTraceElement trace = Thread.currentThread().getStackTrace()[4];
        return trace.getFileName() + " ===> " + trace.getClassName() + " ===> " + trace.getMethodName() + " ===> " + trace.getLineNumber() + "\n";
    }

}
