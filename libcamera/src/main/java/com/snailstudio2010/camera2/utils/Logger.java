package com.snailstudio2010.camera2.utils;

import android.util.Log;

import java.util.Arrays;

@SuppressWarnings("unused")
public class Logger {
    public static boolean enabled = false;

    public static void e(String tag, String msg) {
        if (!enabled) return;
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        if (!enabled) return;
        Log.e(tag, msg, e);
    }

    public static void d(String tag, String msg) {
        if (!enabled) return;
        Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable e) {
        if (!enabled) return;
        Log.d(tag, msg, e);
    }

    public static void d(String tag, String msg, Object[] arr) {
        if (!enabled) return;
        Log.d(tag, msg + ":" + Arrays.toString(arr));
    }
}
