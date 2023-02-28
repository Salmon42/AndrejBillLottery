package com.andrejhucko.andrej.fragments;

import java.util.*;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Context;
import android.support.annotation.*;
import android.content.SharedPreferences;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

public final class AccountFrag extends BaseFragment {

    private static final String TAG = "AccountFrag";
    private Listener listener;
    private LoginDialog loginDialog;
    private LoginDlgStorage lds;

    public interface Listener {
        void restartFragment();
        void confirmAccountDeletion(ConfirmDialog dialog);
        void reloadSideBar();
    }

    public static class Preference extends PreferenceFragmentCompat {

        /// Passed reference to listener into static class by setter
        private Listener listener = null;
        public void setListener (Listener listener) {
            this.listener = listener;
        }

        private SharedPreferences data;
        public void setPreferences (SharedPreferences data) {
            this.data = data;
        }

        SharedPreferences.OnSharedPreferenceChangeListener prefListener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {

            /**
             * After user chooses another active account, the whole fragment gets a redraw.
             * For sake of removal of fragment multiple re-attaching, this listener performs
             * unregistering from the sharedpreference listener. that was creepy
             *
             * @param sharedPreferences idk which one, probs the same as this.data
             * @param s                 key in that sharedPrefs, the only used here is "current_account"
             */
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.equals(DefStorage.CURRENT_ACC) && listener != null) {
                    listener.restartFragment();
                    data.unregisterOnSharedPreferenceChangeListener(prefListener);
                }
            }
        };

        /**
         * Set preference XML.
         * Called when childfragmentmanager replaces part of account fragment with this fragment
         *
         * If the app has no stored users, disable the listpreference, it would show empty list
         * Safety checks for this.data because of some warnings idk
         *
         * @param savedInstanceState classic onCreate
         * @param rootKey            to be honest I have no idea
         */
        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.account_preferences, rootKey);
            final ListPreference listPref = (ListPreference) findPreference(DefStorage.CURRENT_ACC);

            if (data == null || data.getStringSet(DefStorage.ACC_LIST, null) == null) {
                listPref.setEnabled(false);
                return;
            }

            Set<String> accountList = data.getStringSet(DefStorage.ACC_LIST, new HashSet<String>());
            CharSequence[] entries = accountList.toArray(new CharSequence[accountList.size()]);

            listPref.setEntries(entries);
            listPref.setEntryValues(entries);

            data.registerOnSharedPreferenceChangeListener(prefListener);
        }

    }

    public AccountFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__account;
        super.fragmentIndex = FragInfo.Account.pos();
        loginDialog = null;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setComponents(LoginDlgStorage lds) {
        this.lds = lds;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null || getActivity() == null) return;

        final Context context = getContext();
        final Activity activity = getActivity();
        final DefStorage store = Storage.def(context);

        Preference innerPreferenceFragment = new Preference();
        innerPreferenceFragment.setListener(listener);

        innerPreferenceFragment.setPreferences(
                PreferenceManager.getDefaultSharedPreferences(context)
        );

        view.findViewById(R.id.maf_add_acc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginDialog = loginDialogFactory(activity);
                loginDialog.show();
            }
        });

        view.findViewById(R.id.maf_current_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (store.getCurrentAcc() == null) return;

                final ConfirmDialog dialog = new ConfirmDialog(context,
                        R.string.frag_account_dlg_delete_acc,
                        R.string.frag_account_dlg_delete_acc_desc
                );

                dialog.setPositiveButton(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        App.purgeAccountData(context);
                        AppToast.show(context, R.string.frag_account_toast_delete_acc);
                        listener.reloadSideBar();
                        dialog.dismiss();
                    }
                });
                listener.confirmAccountDeletion(dialog);
            }
        });

        view.findViewById(R.id.maf_current_update).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (store.getCurrentAcc() == null) return;
                String user = store.getCurrentAcc();
                String type = store.getAccType(user);

                if (type.equals("E-mail")) {
                    lds.setData(new String[]{user, "", ""});
                }
                else {
                    lds.setData(new String[]{"", user, ""});
                }

                lds.shouldReappear(true);

                loginDialog = loginDialogFactory(activity);
                loginDialog.show();
            }
        });

        ((TextView) view.findViewById(R.id.maf_header_account))
            .setText(store.getCurrentAccPrintable(
                view.getResources().getString(R.string.frag_account_noaccount)
            )
        );

        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.maf_preferences, innerPreferenceFragment)
            .commit();

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
        super.onPause();
    }

    @Override
    public void onResume() {
        if (loginDialog != null) loginDialog.show();
        super.onResume();
    }

    private LoginDialog loginDialogFactory(final Activity context) {
        return new LoginDialog(context, lds, new LoginDialog.Listener() {
            @Override
            public void onFinish(boolean success) {
                if (success && listener != null) {
                    listener.reloadSideBar();
                    listener.restartFragment();
                }
                loginDialog = null;
            }
        });
    }

}
