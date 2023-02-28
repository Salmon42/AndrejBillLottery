package com.andrejhucko.andrej.frontend;

import android.util.Log;
import android.support.v7.widget.RecyclerView;

/**
 * Class for lazy init of RecyclerView
 * Required as the view needs to be final (passed to callbacks inside listeners)
 */
public class LazyRecyclerView {
    private static final String TAG = "LazyRecyclerView";
    private RecyclerView view;

    public RecyclerView get() {
        if (view == null) Log.wtf(TAG, "get() ~ UPCOMING NULLPTR EXCEPTION");
        return view;
    }

    public void set(RecyclerView view) {
        this.view = view;
    }
}
