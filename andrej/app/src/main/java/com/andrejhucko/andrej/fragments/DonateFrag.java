package com.andrejhucko.andrej.fragments;

import com.andrejhucko.andrej.R;

public final class DonateFrag extends BaseFragment {

    private static final String TAG = "DonateFrag";

    public DonateFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__donate;
        super.fragmentIndex = FragInfo.Donate.pos();
    }

}
