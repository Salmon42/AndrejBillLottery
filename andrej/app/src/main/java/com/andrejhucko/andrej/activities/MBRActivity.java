package com.andrejhucko.andrej.activities;

import org.json.*;
import java.util.*;
import android.view.*;
import android.util.Log;
import android.os.Bundle;
import android.widget.TextView;
import android.app.ProgressDialog;
import com.crashlytics.android.Crashlytics;
import android.support.v7.app.AppCompatActivity;
import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.network.*;
import com.andrejhucko.andrej.frontend.manual.*;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.andrejhucko.andrej.backend.utility.App;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_BAD_TOKENS;

public final class MBRActivity extends AppCompatActivity {

    private static final String TAG = "MBRActivity";
    private static BillManager manager = null;
    private Andrej me;

    /** Information dialog about bill being rejected.                                             */
    private InfoDialog rejectedDlg;
    /** Dialog prompt whether user wants to reset all scanned entries and start over.             */
    private ConfirmDialog resetDlg = null;
    /** Dialog prompt whether user wants to exit scanning activity completely.                    */
    private ConfirmDialog exitDlg = null;
    /** Base class object for all manual-entry dialogs. Together with listener to update UI       */
    private static ManualEntryDialog manualDialog = null;
    private final ManualEntryDialog.Listener mdListener = new ManualEntryDialog.Listener() {
        @Override
        public void onFinish(Entry entry) {
            if (entry != null) {
                setFormItem(entry);
            }
            manualDialog = null;
        }
    };

    /** Map containing integer ID's to textviews & strings for corresponding data                 */
    private static final Map<Entry, Integer[]> viewMap;
    static {
        viewMap = new HashMap<>();
        viewMap.put(Entry.FIK, new Integer[] {R.id.ambr_id, R.id.ambr_id_title, R.string.bill_fik});
        viewMap.put(Entry.BKP, new Integer[] {R.id.ambr_id, R.id.ambr_id_title, R.string.bill_bkp});
        viewMap.put(Entry.DATE, new Integer[] {R.id.ambr_date});
        viewMap.put(Entry.TIME, new Integer[] {R.id.ambr_time});
        viewMap.put(Entry.SUM, new Integer[] {R.id.ambr_sum});
        viewMap.put(Entry.MODE, new Integer[] {R.id.ambr_mode});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mbr);

        if (manager == null) manager = new BillManager(false);
        me = (Andrej) getApplication();

        findViewById(R.id.ambr_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!manager.isComplete()) {
                    AppToast.show(MBRActivity.this, R.string.act_mbr_toast_missing);
                    return;
                }
                registerBill();
            }
        });

        findViewById(R.id.ambr_id_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean repairing = manager.isScanned(Entry.FIK) || manager.isScanned(Entry.BKP);
                popUpManualEntry(Entry.FIK, repairing);
            }
        });

        findViewById(R.id.ambr_date_layout).setOnClickListener(oclGen(Entry.DATE));
        findViewById(R.id.ambr_time_layout).setOnClickListener(oclGen(Entry.TIME));
        findViewById(R.id.ambr_sum_layout).setOnClickListener(oclGen(Entry.SUM));
        findViewById(R.id.ambr_mode_layout).setOnClickListener(oclGen(Entry.MODE));

        if (getIntent() != null) {
            String billdata = getIntent().getStringExtra("BILL_FROM_DETAIL");
            if (billdata != null) {
                try {
                    JSONObject billJSON = new JSONObject(billdata);

                    if (billJSON.has(JsonKey.FIK.n())) {
                        manager.setManualEntry(Entry.FIK, billJSON.getString(JsonKey.FIK.n()));
                    }
                    if (billJSON.has(JsonKey.BKP.n())) {
                        manager.setManualEntry(Entry.BKP, billJSON.getString(JsonKey.BKP.n()));
                    }
                    if (billJSON.has(JsonKey.AMOUNT.n())) {
                        manager.setManualEntry(Entry.SUM, Integer.parseInt(billJSON.getString(JsonKey.AMOUNT.n())));
                    }
                    if (billJSON.has(JsonKey.MODE.n())) {
                        manager.setManualEntry(Entry.MODE, billJSON.getBoolean(JsonKey.MODE.n()));
                    }
                    if (billJSON.has(JsonKey.DATE.n())) {
                        manager.setManualEntry(Entry.DATE, billJSON.getString(JsonKey.DATE.n()));
                    }
                    if (billJSON.has(JsonKey.TIME.n())) {
                        manager.setManualEntry(Entry.TIME, billJSON.getString(JsonKey.TIME.n()));
                    }

                    App.removeBill(this, manager);
                    for (Entry e : Entry.values()) setFormItem(e);
                }
                catch (JSONException e) {
                    Log.wtf(TAG, "JSONException");
                    Log.wtf(TAG, e.getMessage());
                }
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fill_bill, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_reg_bill_reset) {
            resetDlg = new ConfirmDialog(this, R.string.act_scan_reset_dlg, R.string.act_scan_reset_dlg_desc);
            resetDlg.setNegativeButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetDlg.dismiss();
                    resetDlg = null;
                }
            });

            resetDlg.setPositiveButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    manager = new BillManager(false);
                    for (Entry e : Entry.values()) setFormItem(e);
                    resetDlg.dismiss();
                    resetDlg = null;
                }
            });
            resetDlg.show();
            return true;
        }
        else if (item.getOrder() == 0) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        AppDialog.destroy(exitDlg, resetDlg, rejectedDlg);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        exitDlg = new ConfirmDialog(this, R.string.act_mbr_exit, R.string.act_mbr_exit_desc);
        exitDlg.setNegativeButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDlg.dismiss();
            }
        });

        exitDlg.setPositiveButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDlg.dismiss();
                App.storeBill(MBRActivity.this, manager);
                shutdownActivity(App.ASR_DO_NOT_RESPOND);
            }
        });
        exitDlg.show();
    }

    @Override
    protected void onResume() {
        if (manualDialog != null) {
            manualDialog.show(this, manager, mdListener);
        }
        super.onResume();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Custom methods
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /**
     * Set content to the form line
     * @param entry which type of entry window is that
     */
    private void setFormItem(Entry entry) {
        if (entry == Entry.DIC || entry == Entry.WORD)
            return;

        final TextView tv = findViewById(viewMap.get(entry)[0]);
        String data = manager.getDispEntry(entry);
        if (manager.isScanned(entry)) {
            tv.setText(data);
        }
        else {
            tv.setText("");
        }

        if (entry == Entry.FIK || entry == Entry.BKP) {
            final TextView t = findViewById(viewMap.get(entry)[1]);
            if (manager.isScanned(entry)) {
                t.setText(viewMap.get(entry)[2]);
            }
            else {
                t.setText(R.string.bill_id);
            }
        }
    }

    /**
     * Show manual input dialog.
     * @param entry     which entry should be modified
     * @param repairing true if a value is scanned, false if user inputs new value
     */
    private void popUpManualEntry(Entry entry, boolean repairing) {

        switch (entry) {
            case BKP: case FIK: manualDialog = new CodeDialog();    break;
            case DATE:          manualDialog = new DateDialog();    break;
            case TIME:          manualDialog = new TimeDialog();    break;
            case SUM:           manualDialog = new SumDialog();     break;
            case MODE:          manualDialog = new ModeDialog();    break;
            default: return;
        }
        manualDialog.show(this, manager, mdListener, repairing);

    }

    /** Register bill (in same manner as like in scanactivity) */
    private void registerBill() {

        if (!App.isConnected(MBRActivity.this)) {
            AppToast.show(MBRActivity.this, R.string.net_err_toast);
            shutdownActivity(App.ASR_NETWORK_ERR);
            return;
        }

        //JSONObject details = LotteryConnection.fetchCredentials(MBRActivity.this); // details == null
        final String user = me.st().def().getCurrentAcc();
        if (user == null) {
            App.storeBill(this, manager); // No added account -> store bill
            shutdownActivity(App.ASR_NO_ACCOUNT);
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(MBRActivity.this);
        progressDialog.setMessage(getString(R.string.act_scan_progdlg_msg));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Async task to send it
        LotteryTask registerTask = new LotteryTask(MBRActivity.this, user);
        registerTask.setManager(manager);
        registerTask.setListener(new LotteryTask.Listener() {
            @Override
            public void onReceivedResult(Response response) {
                progressDialog.dismiss();
                if (response.code() == 201 || response.code() == 400) {
                    try {
                        Status s = Status.get(response.json().getString(JsonKey.STATUS.n()));
                        switch (s) {
                            case NEW:
                            case VERIFIED:
                                // another bill was marked as "NEW" or sth like that, ++ for TBC
                                final int tbc = me.st().def().getTbc(user);
                                me.st().def().setTbc(user, tbc + 1);

                                App.storeBill(MBRActivity.this, manager);
                                LotteryTask task = new LotteryTask(MBRActivity.this, user);
                                task.execute(LotteryTask.Type.GET_BILLS);
                                shutdownActivity(App.ASR_SUCCESS);
                            return;

                            case REJECTED:
                                rejectedDlg = new InfoDialog(MBRActivity.this, true,
                                R.string.act_scan_rejected_bill, R.string.act_scan_rejected_bill_desc);
                                rejectedDlg.show();
                            break;

                            case DUPLICATE: shutdownActivity(App.ASR_DUPLICATE); return;
                            case OUTDATED:  shutdownActivity(App.ASR_OUTDATED);  return;
                            case UNHANDLED: shutdownActivity(App.ASR_UNEXPECTED);
                        }

                    }
                    catch (JSONException e) {
                        Crashlytics.setString("response", response.toString());
                        Crashlytics.setString("bill", manager.getRegPayload());
                        Crashlytics.logException(e);
                        shutdownActivity(App.ASR_UNEXPECTED);
                    }

                }
                else {
                    if (response.code() == RET_BAD_TOKENS) {
                        shutdownActivity(App.ASR_RELOG);
                    }
                    else {
                        shutdownActivity(App.ASR_UNEXPECTED);
                    }
                }
            }
        });
        registerTask.execute(LotteryTask.Type.REG_BILL);
    }

    /** Shut down the activity with given result code */
    private void shutdownActivity(int result) {
        manager = null;
        setResult(result);
        finish();
    }

    /** OnClickListener Generator - for given entry */
    View.OnClickListener oclGen(final Entry entry) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popUpManualEntry(entry, manager.isScanned(entry));
            }
        };
    }

}
