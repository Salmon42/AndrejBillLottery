package com.andrejhucko.andrej.fragments;

import android.view.*;
import android.os.Bundle;
import android.support.annotation.*;
import android.support.v4.app.Fragment;

public class BaseFragment extends Fragment {

    public String fragmentTitle;
    public String fragmentName;
    public int fragmentIndex;
    public int layoutID;

    public String getName() {
        return fragmentName;
    }

    public int getIndex() {
        return fragmentIndex;
    }

    public void setTitle(String Title) {
        fragmentTitle = Title;
    }

    public String getFragmentTitle() {
        return fragmentTitle;
    }

    public String getFragmentName() {
        return fragmentName;
    }

    public FragInfo getInfo() {
        return FragInfo.convert(fragmentIndex);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layoutID, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
