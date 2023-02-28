package com.andrejhucko.andrej.activities;
// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import org.json.*;
import java.util.*;
import android.view.*;
import android.util.Log;
import android.os.Bundle;
import android.animation.*;
import java.io.IOException;
import android.content.pm.*;
import android.widget.TextView;
import android.content.Context;
import android.view.animation.*;
import android.app.ProgressDialog;
import android.support.v4.util.Pair;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.crashlytics.android.Crashlytics;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.FloatingActionButton;
import com.google.firebase.ml.vision.text.FirebaseVisionText.TextBlock;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;

import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.camera.*;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.backend.network.*;
import com.andrejhucko.andrej.frontend.manual.*;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.andrejhucko.andrej.backend.ocr.TextRecognitionProcessor;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_BAD_TOKENS;

public final class ScanActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback {

    private static final String TAG = "ScanActivity";

    /** For permission requests */
    private static final int PERMISSION_REQUESTS = 1;

    /** Map containing integer ID's to textviews & strings for corresponding data                 */
    private static final Map<Entry, Integer[]> liveViewMap;
    static {
        liveViewMap = new HashMap<>();
        // value[0] => Left textview with text
        // value[1] => Left textview without text
        // value[2] => Right textview
        liveViewMap.put(Entry.FIK,  new Integer[] {R.id.locr_tv_id, R.id.locr_tv_id_circle, R.id.locr_tv_id_show});
        liveViewMap.put(Entry.BKP,  new Integer[] {R.id.locr_tv_id, R.id.locr_tv_id_circle, R.id.locr_tv_id_show});
        liveViewMap.put(Entry.DATE, new Integer[] {R.id.locr_tv_date, R.id.locr_tv_date_circle, R.id.locr_tv_date_show});
        liveViewMap.put(Entry.TIME, new Integer[] {R.id.locr_tv_time, R.id.locr_tv_time_circle, R.id.locr_tv_time_show});
        liveViewMap.put(Entry.SUM,  new Integer[] {R.id.locr_tv_price, R.id.locr_tv_price_circle, R.id.locr_tv_price_show});
        liveViewMap.put(Entry.MODE, new Integer[] {R.id.locr_tv_mode, R.id.locr_tv_mode_circle, R.id.locr_tv_mode_show});
        liveViewMap.put(Entry.DIC,  new Integer[] {R.id.locr_tv_dic, R.id.locr_tv_dic_circle, R.id.locr_tv_dic_show});
    }

    /** Universal helper object.                                                                  */
    private Andrej me;

    /** Camera-related attributes.                                                                */
    private CameraSource cameraSource;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private GestureDetector gestureDetector;
    private boolean cameraRunning;

    /** Data extraction attributes.                                                               */
    private static BillManager billManager;
    private boolean extendedScanning;

    /** 12 TextViews handled as tripples, with ability to be shrunk (partially hidden)            */
    private static boolean areViewsHidden;
    private boolean shouldAnimateViews;
    private Animation fadeIn;
    private Animation fadeOut;

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Other objects

    /** Information dialog about bill being rejected.                                             */
    private InfoDialog rejectedDlg;
    /** Dialog prompt whether user wants to reset all scanned entries and start over.             */
    private ConfirmDialog resetDlg = null;
    /** Dialog prompt whether user wants to exit scanning activity completely.                    */
    private ConfirmDialog exitDlg = null;
    /** Overlay screen showing detail of entry with actions (fix, rescan)                         */
    private ConfirmDialog peekDlg = null;

    /** Base class object for all manual-entry dialogs. Together with listener to update UI       */
    private static ManualEntryDialog manualDialog = null;
    private final ManualEntryDialog.Listener mdListener = new ManualEntryDialog.Listener() {
        @Override
        public void onFinish(Entry entry) {
            if (entry != null) {
                redrawUserInterface(entry);
            }
            if (!billManager.isComplete()) {
                startCameraSource();
            }
            else {
                onCompleteScan();
            }
            manualDialog = null;
        }
    };

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Lifecycle methods
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        me = (Andrej) getApplication();
        preview = findViewById(R.id.ocr_scan_preview);
        graphicOverlay = findViewById(R.id.ocr_scan_overlay);

        if (allPermissionsGranted()) {
            createCameraSource();
        } else {
            getRuntimePermissions();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        extendedScanning = me.st().def().getSetting(DefStorage.SETTINGS.EXTSCAN, false);
        if (billManager == null) billManager = new BillManager(extendedScanning);

        fadeIn = AnimationUtils.loadAnimation(ScanActivity.this, R.anim.fade_in_scanview);
        fadeOut = AnimationUtils.loadAnimation(ScanActivity.this, R.anim.fade_out_scanview);

        // areViewsHidden = false; -> intentional
        shouldAnimateViews = false;

        findViewById(R.id.locr_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishScanning();
            }
        });

        if (extendedScanning) {
            findViewById(R.id.locr_tv_dic).setVisibility(View.VISIBLE);
        }

    }

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (manualDialog != null) {
            manualDialog.show(this, billManager, mdListener);
        }
        else if (billManager != null && !billManager.isComplete()) {
            startCameraSource();
        }
        redrawUserInterface(); // redraw all
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {

        AppDialog.destroy(peekDlg, exitDlg, resetDlg, rejectedDlg);
        ManualEntryDialog.destroy(manualDialog);
        if (cameraSource != null) cameraSource.turnFlashOff();

        stopCameraSource();
        super.onPause();
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors,
     * and the rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fill_bill, menu);
        getMenuInflater().inflate(R.menu.menu_scanning, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  UI methods
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /**
     * Update currently recognized fields.
     */
    private void redrawUserInterface(Entry ... entries) {

        if (billManager != null) {

            if (entries.length == 0)
                for (Entry key : liveViewMap.keySet()) setLiveViews(key);
            else for (Entry key : entries) setLiveViews(key);

            shouldAnimateViews = false;

            FloatingActionButton f = findViewById(R.id.locr_send);
            if (billManager.isComplete()) {
                f.setVisibility(View.VISIBLE);
                stopCameraSource();
            }
            else {
                f.setVisibility(View.INVISIBLE);
                startCameraSource();
            }
        }
        else {
            stopCameraSource();
        }

    }

    /**
     * Change textView drawable from loading state to finished state
     * and content to current scanned value (the textview on right side)
     * @param entry which type of entry window is that
     */
    @SuppressWarnings("ConstantConditions")
    private void setLiveViews(Entry entry) {

        final TextView[] tvs = {
            findViewById(liveViewMap.get(entry)[0]), // Left view with text
            findViewById(liveViewMap.get(entry)[1]), // Left view without text
            findViewById(liveViewMap.get(entry)[2]), // Right view
        };

        // Default set strId & content
        //int strId = 0;
        String content = "";
        boolean isEntryScanned;

        // Find out what content should be places into
        // right-side-views & left-side ID textview
        if (entry == Entry.FIK || entry == Entry.BKP) {
            boolean f = billManager.isScanned(Entry.FIK);
            boolean b = billManager.isScanned(Entry.BKP);

            if (!areViewsHidden) {
                if (f) {
                    tvs[0].setText(R.string.act_scan_fik);
                }
                else if (b) {
                    tvs[0].setText(R.string.act_scan_bkp);
                }
                else {
                    tvs[0].setText(R.string.act_scan_id);
                }
            }

            if (f) content = billManager.getDispEntry(Entry.FIK);
            else if (b) content = billManager.getDispEntry(Entry.BKP);

            isEntryScanned = f || b;
        }
        else if (entry == Entry.DIC && !extendedScanning) {
            tvs[0].setVisibility(View.INVISIBLE);
            tvs[1].setVisibility(View.INVISIBLE);
            tvs[2].setVisibility(View.INVISIBLE);
            return;
        }
        else {
            isEntryScanned = billManager.isScanned(entry);
            if (isEntryScanned) {
                content = billManager.getDispEntry(entry);
            }
        }

        // Set the right textview regardless of anything else
        // Can be filled with relevant information or with empty string
        tvs[2].setText(content);

        // Just drawable things.
        Drawable drawable;
        if (isEntryScanned) {
            drawable = ContextCompat.getDrawable(this, R.drawable.tv_loaded);
        }
        else {
            drawable = ContextCompat.getDrawable(this, R.drawable.anim_load);
            ObjectAnimator obj = ObjectAnimator.ofInt(drawable, "level", 5000);
            obj.setRepeatCount(ValueAnimator.INFINITE);
            obj.setDuration(1000);
            obj.start();
        }
        if (areViewsHidden) {
            tvs[1].setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
        else {
            tvs[0].setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
        // End of drawable things

        if (areViewsHidden) {
            // Case 1: left views should be shrunk & right views hidden even when they have data
            if (shouldAnimateViews) {
                // Case 1.1: User tapped on screen in order to hide the views
                tvs[1].startAnimation(fadeIn);
                tvs[1].setVisibility(View.VISIBLE);
                tvs[0].startAnimation(fadeOut);
                tvs[0].setVisibility(View.INVISIBLE);

                if (isEntryScanned) { // vanish the right text
                    tvs[2].startAnimation(fadeOut);
                    tvs[2].setVisibility(View.INVISIBLE);
                }
                else { // keep the right text invisible
                    tvs[2].setVisibility(View.INVISIBLE);
                }
            }
            else {
                // Case 1.2: No user input, but views are hidden
                tvs[0].setVisibility(View.INVISIBLE);   // hide left with text
                tvs[1].setVisibility(View.VISIBLE);     // show left without text
                tvs[2].setVisibility(View.INVISIBLE);   // vanish right
            }
        }
        else {
            // Case 2: left views are expanded and right views shown according to data
            if (shouldAnimateViews) {
                // Case 2.1: User tapped on shrunk views
                tvs[0].startAnimation(fadeIn);
                tvs[0].setVisibility(View.VISIBLE);
                tvs[1].startAnimation(fadeOut);
                tvs[1].setVisibility(View.INVISIBLE);
                if (isEntryScanned) { // fade in the other view
                    tvs[2].startAnimation(fadeIn);
                    tvs[2].setVisibility(View.VISIBLE);
                }
                else { // keep it invisible
                    tvs[2].setVisibility(View.INVISIBLE);
                }
            }
            else {
                // Case 2.2: No user input, views 0 & 2 are shown, view 1 hidden
                tvs[1].setVisibility(View.INVISIBLE);
                if (isEntryScanned) {
                    tvs[2].setVisibility(View.VISIBLE);
                }
                else {
                    tvs[2].setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    /**
     * Shutdown flash and popup toast. Called when BillEntryManager object has recognized
     * all the necessary information, therefore the recognition activity may be finished.
     */
    private void onCompleteScan() {
        if (cameraSource.isFlashOn()) cameraSource.turnFlashOff();
        AppToast.show(this, R.string.act_scan_success);
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
            case DIC:           manualDialog = new VatDialog();     break;
            default: return;
        }

        stopCameraSource();
        manualDialog.show(this, billManager, mdListener, repairing);

    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Activity state methods
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /**
     * Calls finish() and sets result code
     * @param result see {@link App}
     */
    private void shutdownActivity(int result) {
        billManager = null;
        setResult(result);
        finish();
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_opt);
    }

    /**
     * Sends bill in JSON format to lottery server. Uses LotteryAsyncTask for network connection.
     * In case of network failure, the bill JSON is stored in shared preferences for later resend.
     */
    private void finishScanning() {

        if (!App.isConnected(ScanActivity.this)) {

            App.storeBill(this, billManager);
            AppToast.show(ScanActivity.this, R.string.net_err_toast);
            shutdownActivity(App.ASR_NETWORK_ERR);
            return;
        }

        final String user = me.st().def().getCurrentAcc();
        if (user == null) {
            App.storeBill(this, billManager); // No added account -> store bill
            shutdownActivity(App.ASR_NO_ACCOUNT);
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(ScanActivity.this);
        progressDialog.setMessage(getString(R.string.act_scan_progdlg_msg));
        progressDialog.setCancelable(false);
        progressDialog.show();

        LotteryTask registerTask = new LotteryTask(ScanActivity.this, user);
        registerTask.setManager(billManager);
        registerTask.setListener(new LotteryTask.Listener() {
            @Override
            public void onReceivedResult(Response response) {

                if (!isDestroyed() && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (response.code() == 201 || response.code() == 400) {
                    try {
                        Status s = Status.get(response.json().getString(JsonKey.STATUS.n()));
                        switch (s) {
                            case NEW:
                            case VERIFIED:
                                // another bill was marked as "NEW" or sth like that, ++ for TBC
                                final int tbc = me.st().def().getTbc(user);
                                me.st().def().setTbc(user, tbc+1);

                                App.storeBill(ScanActivity.this, billManager);
                                LotteryTask task = new LotteryTask(ScanActivity.this, user);
                                task.execute(LotteryTask.Type.GET_BILLS);
                                shutdownActivity(App.ASR_SUCCESS);
                            return;

                            case REJECTED:
                                rejectedDlg = new InfoDialog(ScanActivity.this, true,
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
                        Crashlytics.setString("bill", billManager.getRegPayload());
                        Crashlytics.logException(e);
                        shutdownActivity(App.ASR_UNEXPECTED);
                    }
                }
                else {
                    if (response.code() == RET_BAD_TOKENS) {
                        App.storeBill(ScanActivity.this, billManager); // Wrong tokens, user has to relog
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

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Camera methods
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {

            cameraSource = new CameraSource(this, graphicOverlay);
            final TextRecognitionProcessor processor = new TextRecognitionProcessor();

            processor.setListener(new TextRecognitionProcessor.Listener() {
                @Override
                public List<TextBlock> parseDetections(List<TextBlock> results) {

                    if (billManager == null || billManager.isComplete()) {
                        return null;
                    }

                    final Pair<HashSet<Entry>, ArrayList<TextBlock>> detections;
                    detections = billManager.parseDetections(results);

                    if (detections == null) {
                        return null;
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (detections.first == null || detections.second == null)
                                    return;

                                redrawUserInterface(detections.first.toArray(new Entry[detections.first.size()]));
                                if (billManager != null && billManager.isComplete()) {
                                    processor.stop();
                                    onCompleteScan();
                                }
                            }
                        });
                        return detections.second;
                    }
                }
            });
            cameraSource.setFrameProcessor(processor);
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (!cameraRunning) {
            if (cameraSource != null) {
                try {
                    if (preview == null) {
                        Log.d(TAG, "resume: Preview is null");
                    }
                    if (graphicOverlay == null) {
                        Log.d(TAG, "resume: graphOverlay is null");
                    }
                    preview.start(cameraSource, graphicOverlay);
                    cameraRunning = true;
                } catch (IOException e) {
                    Log.e(TAG, "Unable to start camera source.", e);
                    cameraSource.release();
                    cameraSource = null;
                }
            }
        }
    }

    private void stopCameraSource() {
        if (cameraRunning) {
            if (preview != null) {
                preview.stop();
                cameraRunning = false;
            }
        }
    }

    private void turnFlash() {
        if (cameraSource != null) {
            if (cameraSource.isFlashOn()) {
                cameraSource.turnFlashOff();
            }
            else {
                cameraSource.turnFlashOn();
            }
        }
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Permission methods
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (permissionNotGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (permissionNotGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean permissionNotGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Touch/input events
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

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
                        billManager = new BillManager(extendedScanning);
                        redrawUserInterface();
                        resetDlg.dismiss();
                        resetDlg = null;
                    }
                });
            resetDlg.show();
            return true;
        }
        else if (item.getItemId() == R.id.menu_scanning_flash) {
            if (!billManager.isComplete()) {
                turnFlash();
            }
            return true;
        }
        else if (item.getItemId() == R.id.menu_scanning_views) {
            areViewsHidden = !areViewsHidden;
            shouldAnimateViews = true;
            redrawUserInterface();
            return true;
        }
        else if (item.getOrder() == 0) {
            onBackPressed();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {

        exitDlg = new ConfirmDialog(this, R.string.act_scan_cancel, R.string.act_scan_cancel_desc);
        exitDlg.setNegativeButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDlg.dismiss();
                exitDlg = null;
            }
        });

        exitDlg.setPositiveButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDlg.dismiss();
                billManager = null;
                setResult(App.ASR_DO_NOT_RESPOND);
                ScanActivity.super.onBackPressed();
            }
        });
        exitDlg.show();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean b = gestureDetector != null && gestureDetector.onTouchEvent(event);
        return b || super.onTouchEvent(event);
    }

    /**
     * Performs action when tapped anywhere on screen (not any views)
     * @return was this action handled?
     */
    private boolean onTap() {
        return true;
    }

    /**
     * Triggered by clicking on liveocr's textviews.
     * @param view toggled textview
     */
    public void onClick(View view) {

        if (billManager == null) return;
        String itemContent;
        String itemTitle;
        final Entry key;

        switch (view.getId()) {
            case R.id.locr_tv_id:
            case R.id.locr_tv_id_circle:
            case R.id.locr_tv_id_show:
                if (billManager.isScanned(Entry.FIK)) {
                    itemTitle = "FIK";
                    key = Entry.FIK;
                }
                else {
                    itemTitle = "BKP";
                    key = Entry.BKP;
                }
                break;

            case R.id.locr_tv_mode:
            case R.id.locr_tv_mode_circle:
            case R.id.locr_tv_mode_show:
                itemTitle = "Režim";
                key = Entry.MODE;
                break;
            case R.id.locr_tv_time:
            case R.id.locr_tv_time_circle:
            case R.id.locr_tv_time_show:
                itemTitle = "Čas";
                key = Entry.TIME;
                break;
            case R.id.locr_tv_date:
            case R.id.locr_tv_date_circle:
            case R.id.locr_tv_date_show:
                itemTitle = "Datum";
                key = Entry.DATE;
                break;
            case R.id.locr_tv_price:
            case R.id.locr_tv_price_circle:
            case R.id.locr_tv_price_show:
                itemTitle = "Cena";
                key = Entry.SUM;
                break;

            case R.id.locr_tv_dic:
            case R.id.locr_tv_dic_circle:
            case R.id.locr_tv_dic_show:
                itemTitle = "DIČ";
                key = Entry.DIC;
                break;

            default: return;
        }

        if (billManager.isScanned(key)) {
            itemContent = billManager.getDispEntry(key);

            peekDlg = new ConfirmDialog(this, getString(R.string.act_scan_mandlg_intro_title, itemTitle), itemContent)
                    .setPositiveButtonText(R.string.act_scan_mandlg_intro_btn_pos)
                    .setNegativeButtonText(R.string.act_scan_mandlg_intro_btn_neg)
                    .setLargeContent(true);

            peekDlg.setPositiveButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    peekDlg.dismiss();
                    billManager.removeEntry(key);
                    redrawUserInterface(key);
                }
            });

            peekDlg.setNegativeButton(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    peekDlg.dismiss();
                    popUpManualEntry(key, true);
                }
            });

            peekDlg.show();
        }
        else {
            popUpManualEntry(key, false);
        }
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Gesture Listener
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap() || super.onSingleTapConfirmed(e);
        }
    }

}
