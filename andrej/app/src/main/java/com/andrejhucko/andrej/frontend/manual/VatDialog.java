package com.andrejhucko.andrej.frontend.manual;

import android.text.Editable;
import android.content.Context;
import android.widget.EditText;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.frontend.dialogs.AppToast;
import com.andrejhucko.andrej.backend.utility.AppTextWatcher;

public class VatDialog extends ManualEntryDialog {

    private String curDic = "";

    public VatDialog() {
        layoutID = R.layout.manual__dic;
    }

    @Override
    public void show(final Context context,
                     final BillManager manager,
                     final Listener listener,
                     boolean repairing) {
        super.show(context, manager, listener, repairing);
        final EditText editDic = getView(R.id.lmv_dlg_edit);

        if (curDic.length() == 0 && repairing) {
            String dic = manager.getDispEntry(Entry.DIC);
            if (dic.substring(0, 2).equals("CZ")) {
                dic = dic.substring(2);
            }
            editDic.setText(dic);
        }
        else {
            editDic.setText(curDic);
        }

        editDic.addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curDic = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 10) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
            }
        });

        alertDialog.get().show();
        editDic.requestFocus();

    }

    @Override
    void onDialogFinish(final Context context, final BillManager manager, final Listener listener) {

        final EditText editDic = getView(R.id.lmv_dlg_edit);
        String dic = editDic.getText().toString();
        if (dic.length() > 10 || dic.length() < 8) {
            AppToast.show(context, R.string.act_scan_manual_dic_toast);
            return;
        }
        dic = "CZ" + dic;
        manager.setManualEntry(Entry.DIC, dic);
        listener.onFinish(Entry.DIC);
        alertDialog.get().dismiss();

    }

}
