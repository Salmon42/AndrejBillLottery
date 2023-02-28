package com.andrejhucko.andrej.frontend;

import android.util.Log;
import android.view.View;

/**
 * Class for Lazy initialisation of View
 * Required as the view needs to be final (passed to callbacks inside listeners)
 */
public class LazyView {
    private static final String TAG = "LazyView";
    private View view;

    public View get() {
        if (view == null) Log.wtf(TAG, "get() ~ UPCOMING NULLPTR EXCEPTION");
        return view;
    }

    public void set(View view) { this.view = view; }

}
