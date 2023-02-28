package com.andrejhucko.andrej.activities;

import android.os.*;
import android.net.Uri;
import android.view.View;
import android.content.Intent;
import android.view.animation.*;
import android.widget.RelativeLayout;
import android.support.v7.app.AppCompatActivity;
import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.backend.network.AppUpdater;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.andrejhucko.andrej.frontend.AppDialog;

public final class IntroActivity extends AppCompatActivity {

    private Animation fade, logo_move;
    private boolean shouldAddAccount;
    private LoginDialog loginDialog;
    private InfoDialog infoDialog;
    private boolean showpolicy;
    private Andrej me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        me = (Andrej) getApplication();
        fade = AnimationUtils.loadAnimation(IntroActivity.this, R.anim.fade_in_fast);
        logo_move = AnimationUtils.loadAnimation(IntroActivity.this, R.anim.logo_move);
        shouldAddAccount = !me.st().def().hasAnyAccount();
        showpolicy = Storage.def(this).isThisFirstRun();

        if (shouldAddAccount) {

            findViewById(R.id.intro_login).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Don't add account if there's no internet connection
                    if (!App.isConnected(IntroActivity.this)) {
                        infoDialog = new InfoDialog(IntroActivity.this, false,
                                R.string.net_err, R.string.net_err_gen);
                        infoDialog.show();
                    }
                    else {
                        LoginDialog.destroy(loginDialog);
                        loginDialog = loginDialogFactory();
                        loginDialog.show();
                    }
                }
            });

            findViewById(R.id.intro_register).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Don't open any browser if there's no internet connection
                    if (!App.isConnected(IntroActivity.this)) {
                        infoDialog = new InfoDialog(IntroActivity.this, false,
                                R.string.net_err, R.string.net_err_gen);
                        infoDialog.show();
                    }
                    else {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.uctenkovka_reglink)));
                        startActivity(browserIntent);
                    }
                }
            });

            findViewById(R.id.intro_later).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    continueToApplication();
                }
            });
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        if (!showpolicy) {
            boolean tried = AppUpdater.checkForUpdate(me.introUpdate(), this, new AppUpdater.Listener() {
                @Override
                public void afterCheck(boolean shouldUpdate) {
                    if (shouldUpdate) {
                        me.appShouldUpdate = true;
                        onUpdate();
                    }
                    else {
                        basicResume();
                    }
                }
            });

            if (!tried && shouldAddAccount) {
                basicLoginScreen();
            }
        }
        else {
            basicResume();
        }
    }

    @Override
    protected void onPause() {
        LoginDialog.destroy(loginDialog);
        AppDialog.destroy(infoDialog);
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (showpolicy) {
            infoDialog = new InfoDialog(this, false,
                    R.string.frag_about_btn_policy, App.genPolicyText());

            infoDialog.setListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppDialog.destroy(infoDialog);
                    showpolicy = false;
                    Storage.def(IntroActivity.this).setFirstRun();
                }
            });
            infoDialog.show();
        }
        else if (me.introUpdateDialog) {
            onUpdate();
        }
        super.onResume();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Custom procedures
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /** Run base activity (with or without scan flag) and shutdown this */
    private void continueToApplication() {

        if (me.st().def().getSetting(DefStorage.SETTINGS.STARTCAM, false)) {
            // Start LiveOCRActivity
            Intent i = new Intent(IntroActivity.this, BaseActivity.class);
            i.putExtra(App.RUN_SCAN, true);
            startActivity(i);
        }
        else {
            // Start BaseActivity
            startActivity(new Intent(IntroActivity.this, BaseActivity.class));
        }

        finish();
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_opt);
    }


    private LoginDialog loginDialogFactory() {
        return new LoginDialog(IntroActivity.this, me.lds(), new LoginDialog.Listener() {
            @Override
            public void onFinish(boolean success) {
                if (success) continueToApplication();
                loginDialog = null;
            }
        });
    }

    /** Pops update dialog. */
    private void onUpdate() {

        AppDialog.destroy(infoDialog);
        infoDialog = new InfoDialog(this, false, R.string.dlg__update, R.string.dlg__update_desc);
        infoDialog.setListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppDialog.destroy(infoDialog);
                infoDialog = null;
                me.introUpdateDialog = false;
                basicResume();
            }
        });
        infoDialog.show();
        me.introUpdateDialog = true;

    }

    /** Default behavior if the user has no account logged in */
    private void basicLoginScreen() {

        final View views[] = {
            findViewById(R.id.intro_drawing),
            findViewById(R.id.intro_actions)
        };

        RelativeLayout.LayoutParams lp;

        lp = new RelativeLayout.LayoutParams(views[0].getLayoutParams());
        lp.removeRule(RelativeLayout.CENTER_IN_PARENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        views[0].setLayoutParams(lp);
        views[0].startAnimation(logo_move);
        views[0].setVisibility(View.VISIBLE);

        // Delayed animations
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                views[1].startAnimation(fade);
                views[1].setVisibility(View.VISIBLE);
            }
        }, 750);

        // check whether the fragment has been recreated with opened logindialog
        // (user entered few chars to edit texts and [rotated screen | hid the app to tray])
        if (me.lds().shouldReappear()) {
            if (loginDialog == null) loginDialog = loginDialogFactory();
            loginDialog.show();
        }
    }

    /** Implements default behavior with animations. */
    private void basicResume() {

        if (shouldAddAccount) {
            basicLoginScreen();
        }
        else {
            View logo = findViewById(R.id.intro_drawing);
            logo.setVisibility(View.VISIBLE);
            logo.startAnimation(fade);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    continueToApplication();
                }
            }, 500);
        }
    }

}
