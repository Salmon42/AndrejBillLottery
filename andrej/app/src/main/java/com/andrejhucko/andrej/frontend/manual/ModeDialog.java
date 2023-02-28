package com.andrejhucko.andrej.frontend.manual;

import android.widget.*;
import android.content.Context;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.bill.*;

public final class ModeDialog extends ManualEntryDialog {

    private boolean checked = false;

    public ModeDialog() {
        layoutID = R.layout.manual__mode;
    }

    @Override
    public void show(final Context context,
                     final BillManager manager,
                     final Listener listener,
                     boolean repairing) {
        super.show(context, manager, listener, repairing);

        final Switch choiceSwitch = getView(R.id.lmm_switch);
        final TextView tv = getView(R.id.lmm_text);
        choiceSwitch.setChecked(checked);
        if (checked) tv.setText(R.string.bill_mode_simple);
        else tv.setText(R.string.bill_mode_normal);

        choiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) tv.setText(R.string.bill_mode_simple);
                else tv.setText(R.string.bill_mode_normal);
                checked = b;
            }
        });
        alertDialog.get().show();

    }

    @Override
    void onDialogFinish(final Context context, final BillManager manager, final Listener listener) {
        manager.setManualEntry(Entry.MODE, checked);
        listener.onFinish(Entry.MODE);
        alertDialog.get().dismiss();
    }

}
