package com.andrejhucko.andrej.frontend.manual;

import android.widget.*;
import android.view.View;
import android.text.Editable;
import android.content.Context;
import java.util.regex.Pattern;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.frontend.dialogs.AppToast;
import com.andrejhucko.andrej.backend.utility.AppTextWatcher;

public final class CodeDialog extends ManualEntryDialog {

    /** FIK & BKP current texts modified by user                                                  */
    private String curFIK[] = {"", "", ""};
    private String curBKP[] = {"", ""};

    /** EditTexts that handles user directly                                                      */
    private final int[] e = {
        R.id.lmfb_edit_fik_part1, R.id.lmfb_edit_fik_part2, R.id.lmfb_edit_fik_part3,
        R.id.lmfb_edit_bkp_part1, R.id.lmfb_edit_bkp_part2
    };

    public CodeDialog() {
        layoutID = R.layout.manual__fik_bkp;
    }

    @Override
    public void show(final Context context,
                     final BillManager manager,
                     final Listener listener,
                     boolean repairing) {
        super.show(context, manager, listener, repairing);

        final EditText fikEdit[] =  { getView(e[0]), getView(e[1]), getView(e[2]) };
        final EditText bkpEdit[] = { getView(e[3]), getView(e[4]) };

        final Switch choiceSwitch = getView(R.id.lmfb_switch);
        choiceSwitch.setChecked(true);

        // Recreated activity? got any unfinished text?
        boolean cleanRun = curBKP[0].length() == 0 && curBKP[1].length() == 0 &&
                curFIK[0].length() == 0 && curFIK[1].length() == 0 && curFIK[2].length() == 0;

        if (cleanRun) {
            // Repairing? paste existing data
            if (repairing) {
                String fikEntry = manager.getDispEntry(Entry.FIK);
                if (fikEntry != null) {
                    String fik[] = fikEntry.split("-");
                    for (int c = 0; c < 3; c++) {
                        fikEdit[c].setText(fik[c]);
                        curFIK[c] = fik[c];
                    }
                }
                String bkpEntry = manager.getDispEntry(Entry.BKP);
                if (bkpEntry != null) {
                    String bkp[] = bkpEntry.split("-");
                    for (int c = 0; c < 2; c++) {
                        bkpEdit[c].setText(bkp[c]);
                        curBKP[c] = bkp[c];
                    }
                }
            }
        }
        else {
            for (int c = 0; c < 3; c++) fikEdit[c].setText(curFIK[c]);
            for (int c = 0; c < 2; c++) bkpEdit[c].setText(curBKP[c]);
        }

        choiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                TextView tv = getView(R.id.lmfb_choice);
                if (b) {
                    tv.setText(R.string.act_scan_manual_bkpfik_fikcode);
                    getView(R.id.lmfb_edit_bkp_layout).setVisibility(View.INVISIBLE);
                    getView(R.id.lmfb_edit_fik_layout).setVisibility(View.VISIBLE);
                    fikEdit[0].requestFocus();
                }
                else {
                    tv.setText(R.string.act_scan_manual_bkpfik_bkpcode);
                    getView(R.id.lmfb_edit_fik_layout).setVisibility(View.INVISIBLE);
                    getView(R.id.lmfb_edit_bkp_layout).setVisibility(View.VISIBLE);
                    bkpEdit[0].requestFocus();
                }
            }
        });

        fikEdit[0].addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curFIK[0] = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 8) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
                else if (len == 8) {
                    fikEdit[1].requestFocus();
                }
            }
        });
        fikEdit[1].addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curFIK[1] = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 4) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
                else if (len == 4) {
                    fikEdit[2].requestFocus();
                }
                else if (len == 0) {
                    fikEdit[0].requestFocus();
                }
            }
        });
        fikEdit[2].addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curFIK[2] = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 4) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
                else if (len == 0) {
                    fikEdit[1].requestFocus();

                }
            }
        });

        bkpEdit[0].addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curBKP[0] = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 8) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
                else if (len == 8) {
                    bkpEdit[1].requestFocus();
                }
            }
        });
        bkpEdit[1].addTextChangedListener(new AppTextWatcher() {
            @Override
            public void store(String s) { curBKP[1] = s; }

            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 8) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                }
                else if (len == 0) {
                    bkpEdit[0].requestFocus();
                }
            }
        });

        alertDialog.get().show();
    }

    @Override
    void onDialogFinish(final Context context, final BillManager manager, final Listener listener) {
        final EditText fikEdit[] =  { getView(e[0]), getView(e[1]), getView(e[2]) };
        final EditText bkpEdit[] = { getView(e[3]), getView(e[4]) };
        final TextView currentChoice = getView(R.id.lmfb_choice);

        if (currentChoice.getText().toString().equals("FIK kód")) {
            Pattern pattern = BillManager.extendedPatterns.get("FIK");
            String fik = fikEdit[0].getText() + "-" + fikEdit[1].getText() + "-" + fikEdit[2].getText();

            if (!pattern.matcher(fik).matches()) {
                AppToast.show(context, R.string.act_scan_manual_bkpfik_toast_fik);
                return;
            }
            manager.setManualEntry(Entry.FIK, fik);
            listener.onFinish(Entry.FIK);
        }
        else {
            Pattern pattern = BillManager.extendedPatterns.get("BKP_CLEAN");
            String bkp = bkpEdit[0].getText() + "-" + bkpEdit[1].getText();

            if (!pattern.matcher(bkp).matches()) {
                AppToast.show(context, R.string.act_scan_manual_bkpfik_toast_bkp);
                return;
            }
            manager.setManualEntry(Entry.BKP, bkp);
            listener.onFinish(Entry.BKP);
        }
        alertDialog.get().dismiss();
    }

}
