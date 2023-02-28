package com.andrejhucko.andrej.frontend.manual;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.content.Context;
import android.widget.EditText;
import java.util.regex.Pattern;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.network.Reporter;
import com.andrejhucko.andrej.frontend.dialogs.AppToast;
import com.andrejhucko.andrej.backend.utility.AppTextWatcher;

public final class SumDialog extends ManualEntryDialog {

    private String curIntp = "";
    private String curDecp = "";
    private final Pattern zeros = Pattern.compile("^0+");
    private final int[] e = { R.id.lms_edit_intpart, R.id.lms_edit_decpart };

    public SumDialog() {
        layoutID = R.layout.manual__sum;
    }

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("ConstantConditions")
    @Override
    public void show(final Context context,
                     final BillManager manager,
                     final Listener listener,
                     boolean repairing) {
        super.show(context, manager, listener, repairing);
        final EditText editInt = getView(e[0]), editDec = getView(e[1]);

        // Repairing? load from manager. Writing sth? reload this
        if (curIntp.length() == 0 && curDecp.length() == 0 && repairing) {
            String sum = manager.getEntry(Entry.SUM).toString();

            // Inspect reason why this sometimes appears to be less than 100
            int last = sum.length() - 2;

            if (last > 0) {
                editInt.setText(sum.substring(0, last));
                editDec.setText(sum.substring(last));
            }
            else {
                editInt.setText("0");
                editDec.setText("0");
                Reporter.send(false, "SumDialog.show(): wrong state", manager.getRegPayload());
            }
        }
        else {
            editInt.setText(curIntp);
            editDec.setText(curDecp);
        }

        editDec.setText("00");
        editInt.addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curIntp = s; }

            @Override
            public void parse(Editable s, int len, int llen) {

                if (len > 5) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }

                String value = s.toString();
                if (zeros.matcher(value).find()) {
                    shouldSkip = true;
                    value = value.replaceAll("^0+", "");
                    s.replace(0, len, value);
                }

            }
        });
        editDec.addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curDecp = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 2) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }

                String value = s.toString();
                if (zeros.matcher(value).find()) {
                    shouldSkip = true;
                    value = value.replaceAll("^0+", "");
                    s.replace(0, len, value);
                }
            }
        });

        alertDialog.get().show();
        editInt.requestFocus();

    }

    @Override
    void onDialogFinish(final Context context, final BillManager manager, final Listener listener) {
        final EditText editInt = getView(e[0]), editDec = getView(e[1]);

        String finalsum = editInt.getText().toString();
        String decpart = editDec.getText().toString();
        if (decpart.length() == 1) decpart += "0";
        finalsum += decpart;

        if (finalsum.length() < 3) {
            AppToast.show(context, R.string.act_scan_manual_sum_toast);
            return;
        }
        manager.setManualEntry(Entry.SUM, Integer.parseInt(finalsum));
        listener.onFinish(Entry.SUM);
        alertDialog.get().dismiss();
    }

}
