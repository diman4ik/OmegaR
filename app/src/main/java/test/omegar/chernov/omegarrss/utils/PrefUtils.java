package test.omegar.chernov.omegarrss.utils;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import test.omegar.chernov.omegarrss.MainApplication;
import test.omegar.chernov.omegarrss.R;

public class PrefUtils {
    public static final String IS_REFRESHING = "IS_REFRESHING";
    public static final String LAST_UPDATE = "LAST_UPDATE";

    public static String getFeedUrl() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getString(MainApplication.getContext().getString(R.string.pref_feedurl_key), MainApplication.getContext().getString(R.string.pref_feedurl_default));
    }

    public static boolean getBoolean(String key, boolean defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getBoolean(key, defValue);
    }

    public static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static long getLong(String key, long defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getLong(key, defValue);
    }

    public static void putLong(String key, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putLong(key, value);
        editor.apply();
    }
}
