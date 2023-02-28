package com.andrejhucko.andrej.frontend;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.activities.BaseActivity;
import com.andrejhucko.andrej.backend.network.*;
import com.andrejhucko.andrej.backend.utility.*;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_NO_CONNECTION;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_MISSING_CURRENT_ACC;

/** UI Wrapper with progressbar for LotteryAsyncTask */
public class NetworkFetch {

    public interface Listener {
        void onFinish(Response response);
    }

    /**
     * Use internet connection to login to uctenkovka server + fetch array of JSON objects
     * store in sharedpreferences "bills"
     * @param activity for getting access to shared preferences
     */
    public static void getDataWithDialog(final Activity activity, final Listener listener) {

        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(activity.getString(R.string.progbar_login));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // https://stackoverflow.com/questions/16046905/android-event-listener-for-app-lifecycle
        activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityPaused(Activity activity) {
                activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

        });

        LotteryTask fetchBillsTask = new LotteryTask(activity, Storage.def(activity).getCurrentAcc());
        fetchBillsTask.setListener(new LotteryTask.Listener() {
            @Override
            public void onReceivedResult(Response response) {
                // https://stackoverflow.com/questions/22924825/view-not-attached-to-window-manager-crash
                if (!activity.isDestroyed() && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                listener.onFinish(response);
            }
        });
        fetchBillsTask.execute(LotteryTask.Type.GET_BILLS);
    }

    /** Same as getDataWithDialog(Context, Listener), but without progressdialog */
    public static void getDataOnBackground(final Activity context, final Listener listener) {

        if (!App.isConnected(context)) {
            listener.onFinish(new Response(RET_NO_CONNECTION));
            return;
        }

        final String user = Storage.def(context).getCurrentAcc();
        if (user == null) {
            listener.onFinish(new Response(RET_MISSING_CURRENT_ACC));
            return;
        }

        LotteryTask task = new LotteryTask(context, user);
        task.setListener(new LotteryTask.Listener() {
            @Override
            public void onReceivedResult(Response response) {
                listener.onFinish(response);
            }
        });
        task.execute(LotteryTask.Type.GET_BILLS);

    }

}

