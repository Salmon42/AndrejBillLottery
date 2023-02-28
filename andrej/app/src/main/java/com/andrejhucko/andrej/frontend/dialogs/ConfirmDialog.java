package com.andrejhucko.andrej.frontend.dialogs;

import android.view.*;
import android.widget.*;
import android.content.*;
import android.util.TypedValue;
import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.frontend.AppDialog;

public final class ConfirmDialog extends AppDialog {

    private Context context;
    private View.OnClickListener positive = null;
    private View.OnClickListener negative = null;

    private Integer posBtnText = null;
    private Integer negBtnText = null;
    private boolean largeContent = false;

    public ConfirmDialog(Context context, Object title, Object message) {
        super(R.id.cdlg_conf_title, R.id.cdlg_conf_message, title, message);
        this.context = context;
    }

    public ConfirmDialog setPositiveButtonText(int resId) {
        posBtnText = resId;
        return this;
    }

    public ConfirmDialog setNegativeButtonText(int resId) {
        negBtnText = resId;
        return this;
    }

    public ConfirmDialog setLargeContent(boolean large) {
        largeContent = large;
        return this;
    }

    public void setPositiveButton(View.OnClickListener listener) {
        this.positive = listener;
    }

    public void setNegativeButton(View.OnClickListener listener) {
        this.negative = listener;
    }

    @SuppressLint("InflateParams")
    public void show() {
        if (!mayShow()) return; // Must-be as first

        // Inflate views
        LayoutInflater inflater = LayoutInflater.from(context);
        dialogView.set(inflater.inflate(R.layout.dialog__confirm, null));
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppRoundDialog);
        alertDialog.set(builder.setView(dialogView.get()).create());

        // broken flaw when user touches on the outside of the dialog - dismissed,
        // but the internal variable isOnScreen stayed untouched
        alertDialog.get().setCanceledOnTouchOutside(true);
        alertDialog.get().setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                isOnScreen = false;
                dialog.dismiss();
            }
        });

        if (largeContent) {
            ((TextView) dialogView.get().findViewById(R.id.cdlg_conf_message))
                .setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }

        Button pos = dialogView.get().findViewById(R.id.cdlg_conf_button_pos);
        Button neg = dialogView.get().findViewById(R.id.cdlg_conf_button_neg);

        if (posBtnText != null && negBtnText != null) {
            LinearLayout ll = dialogView.get().findViewById(R.id.cdlg_conf_button_layout);

            // https://stackoverflow.com/questions/9685658/add-padding-on-view-programmatically
            float scale = context.getResources().getDisplayMetrics().density;
            int dp = (int) (8 * scale + 0.5f);  // 8dp
            ll.setPadding(0, 0, dp*2, dp);

            pos.setText(posBtnText);
            neg.setText(negBtnText);
        }

        // Set OnClickListeners!

        if (positive != null) {
            pos.setOnClickListener(positive);
        }
        else {
            pos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        if (negative != null) {
            neg.setOnClickListener(negative);
        }
        else {
            neg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        super.prepareContent();
        super.show();
    }

    public void dismiss() {
        if (!isOnScreen) return;
        isOnScreen = false;
        alertDialog.get().dismiss();
    }

}
