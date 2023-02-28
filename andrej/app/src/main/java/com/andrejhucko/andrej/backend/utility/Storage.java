package com.andrejhucko.andrej.backend.utility;

import android.content.*;

public class Storage {

    private Context context;
    private DefStorage defaults = null;
    private SharedPreferences unregisteredBills = null;
    private final static int MODE = Context.MODE_PRIVATE;

    public Storage(Context context) {
        this.context = context;
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Default ones

    /** Default shared preferences */
    public DefStorage def() {
        if (defaults == null) {
            defaults = new DefStorage(context);
        }
        return defaults;
    }

    /** Default shared preferences */
    public static DefStorage def(Context context) {
        return new DefStorage(context);
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Registered bills

    /** Registered bills */
    public static SharedPreferences rb(Context context, String prefname) {
        return context.getSharedPreferences("rb_" + prefname, MODE);
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Unregistered bills & Auth

    // Keys: FIK or BKP because it has no identification number (assigned by uctenkovka)
    private static final String PREF_BILL_TO_SEND = "bills_to_send"; // stores bills to be sent

    /** Unregistered bills */
    public SharedPreferences ub() {
        if (unregisteredBills == null) {
            unregisteredBills = context.getSharedPreferences(PREF_BILL_TO_SEND, MODE);
        }
        return unregisteredBills;
    }

    /** Unregistered bills */
    public static SharedPreferences ub(Context context) {
        return context.getSharedPreferences(PREF_BILL_TO_SEND, MODE);
    }

    // Keys: md5 hash of email or phone of user - stores encrypted refresh tokens
    private static final String PREF_AUTH = "auth";

    /** Authorisation tokens storage */
    static SharedPreferences auth(Context context) {
        return context.getSharedPreferences(PREF_AUTH, MODE);
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Manipulations

    public static void remove(SharedPreferences which, String key) {
        if (which == null || key == null) return;
        which.edit().remove(key).apply();
    }

}
