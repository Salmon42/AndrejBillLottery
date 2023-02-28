package com.andrejhucko.andrej.frontend.manual;

import android.content.*;
import android.view.View;
import android.view.LayoutInflater;
import android.support.v7.app.AlertDialog;
import android.support.annotation.CallSuper;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.bill.*;

public class ManualEntryDialog {

    private boolean repairing;
    final LazyAlertDialog alertDialog;
    final LazyView dialogView;
    int layoutID;

    /**
     * Listener for successfull manual entry input for activity
     */
    public interface Listener {
        void onFinish(Entry entry);
    }

    ManualEntryDialog() {
        alertDialog = new LazyAlertDialog();
        dialogView = new LazyView();
    }

    /**
     * Just sweet little wrapper
     * @param id  R.id
     * @param <T> subclass of {@link View}
     * @return View
     */
    final <T extends View> T getView(int id) {
        return dialogView.get().findViewById(id);
    }

    /**
     * Show the dialog, boy.
     * @param context   for alertdialog & other
     * @param manager   for data managing
     * @param listener  activity listener
     * @param repairing state of the dialog
     */
    @CallSuper
    public void show(final Context context, final BillManager manager, final Listener listener, boolean repairing) {
        this.repairing = repairing;

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppRoundDialog);
        dialogView.set(LayoutInflater.from(context).inflate(layoutID, null));
        alertDialog.set(builder.setView(dialogView.get()).create());

        // redundant? idk right now
        alertDialog.get().setCancelable(false);
        alertDialog.get().setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                listener.onFinish(null);
            }
        });

        dialogView.get().findViewById(R.id.mdlg_button_neg).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (alertDialog.get() != null) {
                        alertDialog.get().dismiss();
                        listener.onFinish(null);
                    }
                }
            }
        );

        dialogView.get().findViewById(R.id.mdlg_button_pos).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDialogFinish(context, manager, listener);
                }
            }
        );

    }

    void onDialogFinish(final Context context, final BillManager manager, final Listener listener) {}

    /**
     * Call show again from activity that has been recreated.
     * This recreated activity has no repairing value, but this object has
     * @param context   context for dialogs & toasts
     * @param manager   bill manager for data manipulation
     * @param listener  listener for activity
     */
    public void show(final Context context, final BillManager manager, final Listener listener) {
        this.show(context, manager, listener, this.repairing);
    }

    @CallSuper
    private void onParentDestroyed() {
        alertDialog.get().dismiss();
    }

    public static void destroy(ManualEntryDialog dialog) {
        if (dialog != null) {
            dialog.onParentDestroyed();
        }
    }

    public static void show(ManualEntryDialog dialog, final Context context, final BillManager manager, final Listener listener) {
        if (dialog != null) {
            dialog.show(context, manager, listener);
        }
    }

}
