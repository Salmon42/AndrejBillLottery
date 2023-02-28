package com.andrejhucko.andrej.fragments;

import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.support.annotation.*;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.activities.MBRActivity;
import com.andrejhucko.andrej.activities.ScanActivity;

public final class AddBillFrag extends BaseFragment {

    private static final String TAG = "AddBillFrag";
    private static final int REQUEST_BILL_REG = 0;

    public AddBillFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__add_bill;
        super.fragmentIndex = FragInfo.AddBill.pos();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.abf_button_live).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), ScanActivity.class);
                    getActivity().startActivityForResult(intent, REQUEST_BILL_REG);
                }
            }
        });

        view.findViewById(R.id.abf_button_manual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), MBRActivity.class);
                    getActivity().startActivityForResult(intent, REQUEST_BILL_REG);
                }
            }
        });

    }
}