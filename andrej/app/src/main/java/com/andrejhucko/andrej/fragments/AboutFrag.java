package com.andrejhucko.andrej.fragments;

import android.os.*;
import android.view.View;
import android.support.annotation.*;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.utility.App;

public final class AboutFrag extends BaseFragment {

    private static final String TAG = "AboutFrag";
    private Listener listener;

    public interface Listener {
        void infoDialog(Object title, Object message);
    }

    public AboutFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__about;
        super.fragmentIndex = FragInfo.About.pos();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.af_btn_policy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.infoDialog(R.string.frag_about_btn_policy, App.genPolicyText());
            }
        });

        view.findViewById(R.id.fa_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.infoDialog(R.string.frag_about_easter_egg, R.string.frag_about_easter_egg_desc);
            }
        });

    }
}
