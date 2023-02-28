package com.andrejhucko.andrej.fragments;

import android.widget.*;
import android.os.Bundle;
import android.view.View;
import android.text.Editable;
import android.support.annotation.*;
import android.support.v4.content.ContextCompat;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.network.Reporter;
import com.andrejhucko.andrej.frontend.dialogs.AppToast;
import com.andrejhucko.andrej.backend.utility.AppTextWatcher;

public final class BugReportFrag extends BaseFragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "BugReportFrag";
    private static int currentType = 0;

    /** See {@link R.string} array at fragments */
    private static String[] types = {
        "No choice selected",   // <item>Vyberte typ chyby</item>
        "Scanning bug",         // <item>Skenování</item>
        "Browsing bug",         // <item>Prohlížení</item>
        "Other"                 // <item>Jiné</item>
    };

    public BugReportFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__bug_report;
        super.fragmentIndex = FragInfo.BugReport.pos();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Spinner spinner = view.findViewById(R.id.fbr_dropdown_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(view.getContext(),
                R.array.frag_bugreport_type, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener(this);
        spinner.setAdapter(adapter);

        final Button send = view.findViewById(R.id.fbr_btn_sendreport);
        final EditText editor = view.findViewById(R.id.fbr_report_msg);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppToast.show(getContext(), R.string.frag_bugreport_message_toast);
                Reporter.send(true, types[currentType], editor.getText().toString());
                editor.setText("");
            }
        });

        editor.addTextChangedListener(new AppTextWatcher() {
            @Override
            public void parse(Editable s, int len, int llen) {
                if (len > 500) {
                    shouldSkip = true;
                    s.replace(0, len, lastState);
                    AppToast.show(getContext(), R.string.frag_bugreport_message_maxtoast);
                }
                else if ((len == 0 && llen > 0) || (len > 0 && llen == 0)) {
                    redrawUserInterface();
                }

                setCounter(Integer.toString(len));
            }
        });

        redrawUserInterface();
    }

    public void setCounter(String count) {
        if (getView() == null) return;
        final TextView cnt = getView().findViewById(R.id.fbr_report_msg_cnt);
        cnt.setText(count);
    }

    public void redrawUserInterface() {
        if (getView() == null || getContext() == null) return;
        final Button btn = getView().findViewById(R.id.fbr_btn_sendreport);
        final EditText edit = getView().findViewById(R.id.fbr_report_msg);

        int editlen = edit.getText().length();
        setCounter(Integer.toString(editlen));

        if (currentType > 0 && editlen > 0) {
            btn.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.app_button_shape));
            btn.setClickable(true);
        }
        else {
            btn.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.app_button_disabled_shape));
            btn.setClickable(false);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != currentType) {
            currentType = position;
            redrawUserInterface();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
