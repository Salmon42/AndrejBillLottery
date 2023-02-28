package com.andrejhucko.andrej.frontend.manual;

import android.os.Build;
import java.util.Calendar;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.CalendarView;
import android.support.annotation.NonNull;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.bill.*;

public final class DateDialog extends ManualEntryDialog {

    private final Calendar cal = Calendar.getInstance();

    public DateDialog() {
        layoutID = R.layout.manual__date;
    }

    @Override
    public void show(final Context context,
                     final BillManager manager,
                     final Listener listener,
                     boolean repairing) {
        super.show(context, manager, listener, repairing);

        final CalendarView calView = dialogView.get().findViewById(R.id.lmd_cal);

        // Problems with Android 4.4 & 5.0
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            // Set explicitly the size
            ViewGroup.LayoutParams params = calView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            calView.setLayoutParams(params);
        }

        Calendar calViewSetter = Calendar.getInstance();
        // set currently selected date in the calView
        calView.setDate(cal.getTimeInMillis());

        // User cannot select tommorow as bill date
        calView.setMaxDate(calViewSetter.getTimeInMillis());
        // Setting the the last day in the past that is possible to be registered
        int curDay = calViewSetter.get(Calendar.DAY_OF_MONTH);
        calViewSetter.set(Calendar.DAY_OF_MONTH, 1);
        if (curDay < 15) {
            calViewSetter.set(Calendar.MONTH, calViewSetter.get(Calendar.MONTH) - 1);
        }
        calView.setMinDate(calViewSetter.getTimeInMillis());

        // Store any user-inputs (tap on day)
        calView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            }
        });

        alertDialog.get().show();

    }

    @Override
    void onDialogFinish(final Context context, final BillManager manager, final Listener listener) {
        int y = cal.get(Calendar.YEAR),
            m = cal.get(Calendar.MONTH) + 1,
            d = cal.get(Calendar.DAY_OF_MONTH);

        // Dont mind this obsfucation, just YYYY-MM-DD
        String date = y+"-"+((m<10)?("0"+m):m)+"-"+((d<10)?("0"+d):d);
        manager.setManualEntry(Entry.DATE, date);
        listener.onFinish(Entry.DATE);
        alertDialog.get().dismiss();
    }

}
