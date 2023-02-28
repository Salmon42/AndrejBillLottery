package com.andrejhucko.andrej.frontend.dialogs;

import android.view.*;
import android.content.Context;
import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.frontend.AppDialog;

public final class InfoDialog extends AppDialog {

    private boolean cancelable;
    private Context context;
    private View.OnClickListener listener;

    public InfoDialog(Context context, boolean cancelable, Object title, Object message) {
        super(R.id.idlg_info_title, R.id.idlg_info_message, title, message);
        this.context = context;
        this.cancelable = cancelable;
    }

    public void setListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("InflateParams")
    public void show() {
        if (!mayShow()) return; // Must-be as first

        LayoutInflater inflater = LayoutInflater.from(context);
        dialogView.set(inflater.inflate(R.layout.dialog__info, null));
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppRoundDialog);
        alertDialog.set(builder.setView(dialogView.get()).create());

        if (listener != null) {
            dialogView.get().findViewById(R.id.idlg_info_button).setOnClickListener(listener);
        }
        else {
            dialogView.get().findViewById(R.id.idlg_info_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.get().dismiss();
                }
            });
        }

        alertDialog.get().setCancelable(cancelable);
        super.prepareContent();
        super.show();
    }

}
