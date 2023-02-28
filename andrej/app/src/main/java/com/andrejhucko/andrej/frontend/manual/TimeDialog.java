package com.andrejhucko.andrej.frontend.manual;

import android.text.Editable;
import android.content.Context;
import android.widget.EditText;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.frontend.dialogs.AppToast;
import com.andrejhucko.andrej.backend.utility.AppTextWatcher;

public final class TimeDialog extends ManualEntryDialog {

    private String currentHour = "";
    private String currentMin = "";
    private final int[] e = { R.id.lmt_edit_hr, R.id.lmt_edit_min };

    public TimeDialog() {
        layoutID = R.layout.manual__time;
    }

    @Override
    public void show(final Context context,
                     final BillManager manager,
                     final Listener listener,
                     boolean repairing) {
        super.show(context, manager, listener, repairing);
        final EditText editHour = getView(e[0]), editMin = getView(e[1]);

        if (currentHour.length() == 0 && currentMin.length() == 0 && repairing) {
            String fulltime = manager.getDispEntry(Entry.TIME);
            editHour.setText(fulltime.substring(0, 2));
            editMin.setText(fulltime.substring(3));
        }
        else {
            editHour.setText(currentHour);
            editMin.setText(currentMin);
        }

        editHour.addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { currentHour = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                int value = (len == 0) ? 0 : Integer.parseInt(s.toString());

                if (len > 2) { // Added third char? remove it
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
                else if (len == 2 && llen == 1) { // Entered second digit
                    if (value > 23) {
                        shouldSkip = true;
                        s.delete(llen, len);
                    }
                    else {
                        editMin.requestFocus();
                    }
                }
                else if (len == 1 && value > 2) { // Entered first digit
                    shouldSkip = true;
                    editMin.requestFocus();
                    s.insert(0, "0");
                }
            }
        });
        editMin.addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { currentMin = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                int value = (len == 0) ? 0 : Integer.parseInt(s.toString());

                if (len > 2) { // Added third char? remove it
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
                else if (len == 2 && llen == 1 && value > 59) { // Entered second digit
                    shouldSkip = true;
                    s.delete(llen, len);
                }
                else if (len == 1 && value > 5) { // Entered first digit
                    shouldSkip = true;
                    s.insert(0, "0");
                }
                else if (len == 0) {
                    editHour.requestFocus();
                }
            }
        });

        alertDialog.get().show();
        editHour.requestFocus();

    }

    @Override
    void onDialogFinish(final Context context, final BillManager manager, final Listener listener) {

        final EditText editHour = getView(e[0]), editMin = getView(e[1]);
        String parsed = editHour.getText().toString() + ":" + editMin.getText().toString();
        if (parsed.length() != 5 || parsed.indexOf(":") != 2) {
            AppToast.show(context, R.string.act_scan_manual_time_toast);
            return;
        }
        manager.setManualEntry(Entry.TIME, parsed);
        listener.onFinish(Entry.TIME);
        alertDialog.get().dismiss();

    }

}
