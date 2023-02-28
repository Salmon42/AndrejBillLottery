package com.andrejhucko.andrej.backend.utility;

import android.text.*;

public class AppTextWatcher implements TextWatcher {

    protected boolean shouldSkip = false;
    protected String lastState = "";

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (shouldSkip) shouldSkip = false;
        else {
            parse(s, s.length(), lastState.length());
        }
        lastState = s.toString();
        store(lastState);
    }

    public void store(String s) {}

    public void parse(Editable s, int len, int llen) {}

}
