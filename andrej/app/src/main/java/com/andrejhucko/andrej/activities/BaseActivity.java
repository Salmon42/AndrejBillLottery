package com.andrejhucko.andrej.activities;

import java.util.*;
import android.view.*;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.support.v4.app.*;
import android.support.v7.app.*;
import android.support.design.widget.*;
import android.support.v7.widget.Toolbar;
import android.support.annotation.NonNull;
import com.crashlytics.android.Crashlytics;
import android.view.animation.AnimationUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.content.ContextCompat;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;

import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.fragments.*;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.frontend.AppDialog;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.andrejhucko.andrej.frontend.dialogs.filter.BillFilterChoice;

public final class BaseActivity extends AppCompatActivity implements OnNavigationItemSelectedListener {
    private static final int REQUEST_BILL_REG = 0;

    Andrej me;
    FragmentManager fm;

    NavigationView sideNav;
    DrawerLayout baseLayout;
    FloatingActionButton abTakePhoto;
    InfoDialog infoDialog;
    ConfirmDialog confirmDialog;

    /**
     * Sweet activity lifecycle!
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        me = (Andrej) getApplication();
        fm = getSupportFragmentManager();
        me.setLCOBase(getLifecycle());

        // Orientation change; update references!
        this.reassignFragments();

        baseLayout = findViewById(R.id.activity_base_layout);

        sideNav = findViewById(R.id.nav_view);
        sideNav.setNavigationItemSelectedListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, baseLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        baseLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupFloatBtn();
        updateNavigationHeader();

        if (getIntent() != null) {
            boolean startScan = getIntent().getBooleanExtra(App.RUN_SCAN, false);
            if (startScan) {
                Intent scanActivity = new Intent(BaseActivity.this, ScanActivity.class);
                startActivityForResult(scanActivity, REQUEST_BILL_REG);
            }
        }

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (me.getBackStackSize() == 0) {

            // https://stackoverflow.com/questions/25926402/illegalstateexception-fragment-already-added-in-the-tabhost-fragment
            boolean notAdded = fm.findFragmentByTag(FragInfo.Home.getName()) == null;
            if (notAdded) {
                if (me.setCurrentFrag(FragInfo.Home)) { // Set HomeFrag as default
                    prepareFragment(FragInfo.Home, me.currentFrag());
                }
                // In the drawermenu, auto-select the "Home" section
                sideNav.getMenu().getItem(me.currentFrag().getIndex()).setChecked(true);
                me.addToBackStack(FragInfo.Home);
                fm.beginTransaction()
                        .add(R.id.lab__main_content, me.currentFrag(), me.currentFrag().getFragmentName())
                        .commit();
                fm.executePendingTransactions();
            }
            else {
                // Expected crash code
                Crashlytics.setString("BaseActivity.onPostResume", "HomeFrag error");
                int size = me.getBackStackSize();
                Crashlytics.setInt("Custom backstack size", size);
                String[] frags = new String[size];
                for (int i = 0; i < size; i++) {
                    frags[i] = FragInfo.convert(me.getBSEntryFragmentAt(i)).getName();
                }
                Crashlytics.setString("My fragments (top is rightmost)", Arrays.toString(frags));

                // Hope?
                this.reassignFragments();
            }
        }
        else {

            if (me.isPendingFragmentReload()) {
                refreshCurrentFragment();
            }
            else {
                ArrayDeque<Integer> fragmentsToAdd = me.pendingFragments();
                if (fragmentsToAdd.size() != 0) {
                    for (Integer id : fragmentsToAdd) openFragment(id);
                }
                else {
                    setTitle(me.getFrag(me.getBackStackTop()).getFragmentTitle());
                }
            }
        }
    }

    /**
     * Sweet activity lifecycle, vol. 3
     */
    @Override
    protected void onPause() {
        AppDialog.destroy(infoDialog, confirmDialog);
        super.onPause();
    }

    /**
     * [AL vol. 5] Inflating method (of menu xml's)
     * @param menu XML
     * @return I don't know sorry
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.base, menu);
        return true;
    }

    /**
     * [AL vol. 6] Perform action on action bar click
     * @param item R.id from menus
     * @return was it handled?
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            openFragment(R.id.nav_settings);
            sideNav.getMenu().getItem(me.currentFrag().getIndex()).setChecked(true);
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    /**
     * [AL vol. 7] Perform action on navigation drawer item selected
     * @param item from drawer_base.xml
     * @return was it handled?
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        openFragment(item.getItemId());
        baseLayout.closeDrawer(GravityCompat.START);
        return true;

    }

    /** Custom backstack handling - Popping & hiding fragments, resetting titles, etc. */
    @Override
    public void onBackPressed() {
        if (baseLayout.isDrawerOpen(GravityCompat.START)) {
            baseLayout.closeDrawer(GravityCompat.START);
        } else {
            if (me.getBackStackSize() <= 1) {
                me.popBackStack();
                super.onBackPressed();
            }
            else {
                fm.beginTransaction()
                    .remove(me.currentFrag())
                    .show(me.getFrag(me.getBSEntryFragmentAt(me.getBackStackSize() - 2)))
                    .commit();

                me.popBackStack();
                me.setCurrentFrag(me.getBackStackTop());

                fm.beginTransaction()
                        .detach(me.currentFrag())
                        .attach(me.currentFrag())
                        .commit();

                setTitle(me.currentFrag().getFragmentTitle());
                sideNav.getMenu().getItem(me.currentFrag().getIndex()).setChecked(true);
            }
        }
    }

    /** Handles returning from other activities (e.g. Scan & Reg activity) */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BILL_REG) {

            if (resultCode == App.ASR_SUCCESS) {
                AppToast.show(this, R.string.act_base_toast_success_registration);
            }
            else if (resultCode != App.ASR_DO_NOT_RESPOND) {

                switch (resultCode) {
                    case App.ASR_NETWORK_ERR: popInfoDialog(R.string.net_err, R.string.net_err_desc); break;
                    case App.ASR_NO_ACCOUNT: popInfoDialog(R.string.act_base_dlg_no_acc, R.string.act_base_dlg_no_acc_desc); break;
                    case App.ASR_DUPLICATE: popInfoDialog(R.string.act_scan_progdlg_msg, R.string.act_base_dlg_duplicate); break;
                    case App.ASR_UNEXPECTED: popInfoDialog(R.string.act_scan_progdlg_msg, R.string.act_base_dlg_unexpect); break;
                    case App.ASR_OUTDATED: popInfoDialog(R.string.act_base_dlg_old, R.string.act_base_dlg_old_desc); break;
                    case App.ASR_RELOG:
                        String user = Storage.def(this).getCurrentAcc();
                        if (user != null) Storage.def(this).invalidateUser(user);
                        popInfoDialog(R.string.act_base_dlg_relog, R.string.act_base_dlg_relog_desc);
                        updateNavigationHeader();
                }
            }
        }
        refreshCurrentFragment();

    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Fragment handling
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /**
     * Custom method ensuring non-broken callbacks inside fragments going to front of UI and back
     * @param info            {@link FragInfo} item
     * @param updatedFragment ptr to fragment (used it's superclass on purpose)
     */
    private void prepareFragment(final FragInfo info, final BaseFragment updatedFragment) {

        /// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- -- Home
        if (info == FragInfo.Home) {
            ((HomeFrag) updatedFragment).setComponents(me.st(), me.lds());
            ((HomeFrag) updatedFragment).setListener(new HomeFrag.Listener() {
                @Override
                public void finishLogin() {
                    refreshCurrentFragment();
                    updateNavigationHeader();
                }

                @Override
                public void startBillFragment() {
                    // start bill fragment & set filter to NON-reg only
                    BillFilterChoice choice = new BillFilterChoice(
                            DefStorage.BILL_FILTER.D_UNSENT,
                            DefStorage.BILL_FILTER.P_ALL
                    );

                    Storage.def(BaseActivity.this).setBillFilter(choice);
                    openFragment(R.id.nav_my_bills);
                }

                @Override
                public void startAccountsFragment() {
                    openFragment(R.id.nav_account); // crashes - called after onSaveInstanceState
                }
            });
        }

        if (info == FragInfo.MyBills) {
            ((MyBillsFrag) updatedFragment).setListener(new MyBillsFrag.Listener() {
                @Override
                public void informInvalidUser() {
                    String user = Storage.def(BaseActivity.this).getCurrentAcc();
                    if (user != null) Storage.def(BaseActivity.this).invalidateUser(user);
                    popInfoDialog(R.string.act_base_dlg_relog, R.string.act_base_dlg_relog_update);
                    updateNavigationHeader();
                }
            });
        }

        if (info == FragInfo.Lottery) {
            ((LotteryFrag) updatedFragment).setComponents(me.css());
        }

        /// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- Account
        if (info == FragInfo.Account) {
            ((AccountFrag) updatedFragment).setComponents(me.lds());
            ((AccountFrag) updatedFragment).setListener(new AccountFrag.Listener() {
                @Override
                public void restartFragment() {
                    refreshCurrentFragment();
                }

                @Override
                public void confirmAccountDeletion(ConfirmDialog dialog) {
                    popConfDialog(dialog);
                }

                @Override
                public void reloadSideBar() {
                    updateNavigationHeader();
                }
            });
        }

        /// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- Settings
        if (info == FragInfo.Settings) {
            ((SettingsFrag) updatedFragment).setListener(new SettingsFrag.Listener() {
                @Override
                public void updateFloatButton() {
                    // Animationed callback to show or hide the floating action button.
                    if (!Storage.def(BaseActivity.this).getSetting(DefStorage.SETTINGS.FLOATBTN,true)) {
                        abTakePhoto.startAnimation(AnimationUtils.loadAnimation(BaseActivity.this, R.anim.fab_hide));
                        abTakePhoto.setVisibility(View.INVISIBLE);
                    }
                    else {
                        abTakePhoto.startAnimation(AnimationUtils.loadAnimation(BaseActivity.this, R.anim.fab_show));
                        abTakePhoto.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        if (info == FragInfo.About) {
            ((AboutFrag) updatedFragment).setListener(new AboutFrag.Listener() {
                @Override
                public void infoDialog(Object title, Object message) {
                    popInfoDialog(title, message);
                }

            });
        }

    }

    /**
     * Open fragment given by R.id; if already opened, do nothing.
     * @param id id's from "drawer_base.xml"
     */
    private void openFragment(final int id) {

        if (!me.baseMayShowUp()) {
            me.addPendingFragment(id);
            return;
        }

        boolean swap = false;
        final BaseFragment oldFragment = me.currentFrag();
        final String cname = oldFragment.getName();

        FragInfo searchFragment = FragInfo.getById(id);
        // Confirms swap, prepares callbacks from fragments
        if (!cname.equals(searchFragment.getName())) {
            if (me.setCurrentFrag(searchFragment)) {
                prepareFragment(searchFragment, me.currentFrag());
            }
            swap = true;
        }

        // Performs the fragment switch with custom backstack implementation
        if (swap) {
            try {
                FragmentTransaction tr = fm.beginTransaction();
                tr.hide(oldFragment);
                if (fm.findFragmentByTag(me.currentFrag().getName()) != null) {
                    tr.show(me.currentFrag());
                    me.removeFromBackStack(me.currentFrag().getIndex());
                }
                else {
                    tr.add(R.id.lab__main_content, me.currentFrag(), me.currentFrag().getName());
                }
                tr.commit();
                me.addToBackStack(me.currentFrag().getIndex());
                setTitle(me.currentFrag().getFragmentTitle());
            }
            catch (IllegalStateException e) {
                me.setCurrentFrag(oldFragment.getInfo());
                me.addPendingFragment(id);
            }
        }
    }

    /** Restarts UI of the current fragment. */
    private void refreshCurrentFragment() {
        if (!me.baseMayShowUp()) {
            me.setPendingReload();
            return;
        }
        try {
            fm.beginTransaction()
                .detach(me.currentFrag())
                .attach(me.currentFrag())
                .commit();
        }
        catch (IllegalStateException e) {
            me.setPendingReload();
        }
    }


    private void reassignFragments() {
        if (fm.getFragments() != Collections.EMPTY_LIST) {
            me.updateFragments(fm.getFragments());
            for (int i = 0; i < App.FRAGMENT_COUNT; i++) {
                if (me.getFrag(i) != null) {
                    me.setFragmentTitle(FragInfo.convert(i));
                    prepareFragment(FragInfo.convert(i), me.getFrag(i));
                }
            }
        }
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Code Shortening
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /** Updates the current account in the drawer header */
    private void updateNavigationHeader() {
        if (sideNav.getHeaderCount() == 0) return;
        View header = sideNav.getHeaderView(0);
        DefStorage def = me.st().def();

        String user = def.getCurrentAccPrintable(getString(R.string.frag_home_notlogged));
        ((TextView) header.findViewById(R.id.drawer_hdr_user_content)).setText(user);

        GradientDrawable gDrawable = (GradientDrawable) header.findViewById(R.id.drawer_hdr_user_indicator).getBackground();
        if (!def.isValidUser(user)) {
            gDrawable.setColor(ContextCompat.getColor(this, R.color.billInCheck));
        }
        else if (def.hasAnyAccount()) {
            gDrawable.setColor(ContextCompat.getColor(this, R.color.billIsWinning));
        }
        else {
            gDrawable.setColor(ContextCompat.getColor(this, R.color.billNotRegistered));
        }
    }

    /** Sets up the float action button for scanning */
    private void setupFloatBtn() {
        abTakePhoto = findViewById(R.id.actionCameraButton);
        if (!Storage.def(this).getSetting(DefStorage.SETTINGS.FLOATBTN, true)) {
            abTakePhoto.setVisibility(View.INVISIBLE);
        }
        else {
            abTakePhoto.setVisibility(View.VISIBLE);
        }
        // ----- ----- ----- ----- Callbacks time ----- ----- ----- -----
        // Floating Action Button taking us into another activity
        abTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Intent scanActivity = new Intent(BaseActivity.this, ScanActivity.class);
                startActivityForResult(scanActivity, REQUEST_BILL_REG);
                overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_opt);
            }
        });
    }

    /** Pops up basic infodialog with title and message. */
    private void popInfoDialog(Object title, Object message) {
        infoDialog = new InfoDialog(this, true, title, message);
        infoDialog.show();
    }

    private void popConfDialog(ConfirmDialog dialog) {
        confirmDialog = dialog;
        confirmDialog.show();
    }

}
