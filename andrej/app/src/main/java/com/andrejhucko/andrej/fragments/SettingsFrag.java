package com.andrejhucko.andrej.fragments;

import android.os.Bundle;
import android.view.View;
import android.support.annotation.*;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.utility.DefStorage;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

public final class SettingsFrag extends BaseFragment {

    private static final String TAG = "SettingsFrag";

    public interface Listener {
        void updateFloatButton();
    }

    private Listener listener;
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public static class Preference extends PreferenceFragmentCompat {

        /// Passed reference to listener into static class by setter
        private Listener listener = null;
        public void setListener (Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        }

        @Override
        public boolean onPreferenceTreeClick(android.support.v7.preference.Preference preference) {
            if (preference.getKey().equals(DefStorage.SETTINGS.FLOATBTN)) {
                if (listener != null) {
                    // calls method implemented in BaseActivity
                    listener.updateFloatButton();
                }
            }
            return super.onPreferenceTreeClick(preference);
        }
    }

    public SettingsFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__settings;
        super.fragmentIndex = FragInfo.Settings.pos();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Preference pref = new Preference();
        pref.setListener(listener);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.frag_settings, pref)
                .commit();
    }

}
