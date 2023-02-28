package com.andrejhucko.andrej.frontend;

import android.util.Log;
import android.support.v7.app.AlertDialog;

/**
 * Inner class for Lazy initialisation of AlertDialog
 * Required as the view needs to be final (passed to callbacks inside listeners)
 */
public class LazyAlertDialog {

    private static final String TAG = "LazyAlertDialog";
    private AlertDialog dialog;

    public AlertDialog get() {
        if (dialog == null) Log.wtf(TAG, "get() ~ UPCOMING NULLPTR EXCEPTION");
        return dialog;
    }

    public void set(AlertDialog dialog) {
        this.dialog = dialog;
    }

    public void dismiss() {
        if (dialog != null) dialog.dismiss();
    }

}
