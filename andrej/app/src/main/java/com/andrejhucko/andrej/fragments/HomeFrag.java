package com.andrejhucko.andrej.fragments;

import android.net.Uri;
import android.widget.*;
import android.view.View;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.*;
import android.support.v4.content.ContextCompat;
import android.graphics.drawable.GradientDrawable;
import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.andrejhucko.andrej.backend.utility.App;
import com.andrejhucko.andrej.activities.ScanActivity;

public final class HomeFrag extends BaseFragment {

    private static final String TAG = "HomeFrag";
    private static final int REQUEST_BILL_REG = 0;

    private LoginDialog loginDialog;
    private InfoDialog networkErrDialog;
    private Listener listener;
    private Storage storage;
    private LoginDlgStorage lds;

    public interface Listener {
        /** Called after successfully finishing logging in. */
        void finishLogin();
        /** After choosing to register some unregistered bills (solver btn) */
        void startBillFragment();
        /** After choosing to log in (solver btn) */
        void startAccountsFragment();
    }

    public HomeFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__home;
        super.fragmentIndex = FragInfo.Home.pos();
        loginDialog = null;
    }

    public void setComponents(Storage storage, LoginDlgStorage lds) {
        this.storage = storage;
        this.lds = lds;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null || getActivity() == null) return;
        final Activity activity = getActivity();
        final Andrej app = (Andrej) activity.getApplication();

        // Set next lottery date
        ((TextView) view.findViewById(R.id.hf_curdate)).setText(App.printDate(DrawDate.getNextLotteryDate()));

        // Get ref to the circle drawable, button & text of "info panel"
        GradientDrawable gDrawable = (GradientDrawable) view.findViewById(R.id.hf_indicator).getBackground();
        TextView status = view.findViewById(R.id.hf_status);
        Button solver = view.findViewById(R.id.hf_button_solve);

        String user = storage.def().getCurrentAcc();

        if (app.appShouldUpdate) {
            gDrawable.setColor(ContextCompat.getColor(view.getContext(), R.color.billNotRegistered));
            status.setText(R.string.frag_home_update);
            solver.setText(R.string.frag_home_solver_update);

            solver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // https://stackoverflow.com/questions/11753000/how-to-open-the-google-play-store-directly-from-my-android-application
                    final String appPackageName = app.getPackageName();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }

                }
            });
        }
        else if (user == null) {
            // The user has no added account - no bills obviously
            gDrawable.setColor(ContextCompat.getColor(view.getContext(), R.color.billNotRegistered));
            status.setText(R.string.frag_home_notlogged);
            solver.setText(R.string.login);

            solver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Don't add account if there's no internet connection
                    if (!App.isConnected(v.getContext())) {
                        networkErrDialog = new InfoDialog(getActivity(), false,
                                R.string.net_err, R.string.net_err_gen);
                        networkErrDialog.show();
                    }
                    else {
                        if (app.baseMayShowUp()) {
                            loginDialog = loginDialogFactory(activity);
                            loginDialog.show();
                        }
                    }
                }
            });
        }
        else if (!storage.def().isValidUser(user)) {

            gDrawable.setColor(ContextCompat.getColor(view.getContext(), R.color.billInCheck));
            status.setText(R.string.frag_home_relog);
            solver.setText(R.string.login);
            solver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.startAccountsFragment();
                }
            });

        }
        else if (storage.ub().getAll().size() != 0) {

            // The unreg-bill-shared-prefs are non empty
            gDrawable.setColor(ContextCompat.getColor(view.getContext(), R.color.billInCheck));
            status.setText(getString(R.string.frag_home_unregbill, storage.ub().getAll().size()));
            solver.setText(R.string.register);
            solver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.startBillFragment();
                }
            });

        }
        else {
            gDrawable.setColor(ContextCompat.getColor(view.getContext(), R.color.billIsWinning));
            status.setText(R.string.frag_home_ok);
            solver.setText(R.string.frag_home_solver_play);
            solver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), ScanActivity.class);
                        getActivity().startActivityForResult(intent, REQUEST_BILL_REG);
                    }
                }
            });
        }

        // check whether the fragment has been recreated with opened logindialog
        // (user entered few chars to edit texts and [rotated screen | hid the app to tray])
        if (lds.shouldReappear()) {
            if (loginDialog == null) loginDialog = loginDialogFactory(activity);
            loginDialog.show();
        }
    }

    @Override
    public void onPause() {
        if (loginDialog != null) loginDialog.onParentDestroyed();
        if (networkErrDialog != null) networkErrDialog.onParentDestroyed();
        super.onPause();
    }

    @Override
    public void onResume() {
        if (loginDialog != null) loginDialog.show();
        super.onResume();
    }

    private LoginDialog loginDialogFactory(final Activity activity) {

        return new LoginDialog(activity, lds, new LoginDialog.Listener() {
            @Override
            public void onFinish(boolean success) {
                if (success && listener != null) {
                    listener.finishLogin();
                }
                loginDialog = null;
            }
        });

    }
}
