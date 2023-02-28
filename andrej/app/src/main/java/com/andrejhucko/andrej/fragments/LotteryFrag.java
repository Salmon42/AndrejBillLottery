package com.andrejhucko.andrej.fragments;

import java.util.*;
import android.widget.*;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.content.Context;
import android.support.annotation.*;
import android.annotation.SuppressLint;
import android.support.v4.content.ContextCompat;
import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.backend.bill.Bill;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.andrejhucko.andrej.activities.MBRActivity;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.github.sundeepk.compactcalendarview.CompactCalendarView;

public final class LotteryFrag extends BaseFragment {

    private static final String TAG = "LotteryFrag";
    private static final int REQUEST_BILL_REG = 0;

    /** Used for filling bills floater dialog imitating the little mybills fragment               */
    private List<Bill> bills = null;
    /** Sweet pop-up dialogs                                                                      */
    private BillDetail detail = null;
    private BillsFloater floater = null;
    /** Calendar state keeper                                                                     */
    private CalendarScrollStorage css = null;
    /** Updates on month scroll, keeps current state of calendar in case of rotation              */
    private Date currentShownMonth = null;

    public LotteryFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__lottery;
        super.fragmentIndex = FragInfo.Lottery.pos();
    }

    public void setComponents(CalendarScrollStorage css) {
        this.css = css;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null || getActivity() == null) return;

        final CompactCalendarView compactCalendar = view.findViewById(R.id.fl_calendar);
        final Button showBtn = view.findViewById(R.id.fl_showbtn);
        final TextView monthView = view.findViewById(R.id.fl_monthview),
                       eventDay = view.findViewById(R.id.fl_eventDate),
                       eventCont = view.findViewById(R.id.fl_eventCont);

        final String[] months = getResources().getStringArray(R.array.months);
        final Context context = getContext();

        Calendar calendar = Calendar.getInstance();

        // if victim of rotation, restore state
        if (css != null && css.isSet()) {
            Date restored = css.get();
            calendar.setTime(restored);
            compactCalendar.setCurrentDate(restored);
        }

        // Keep state which month is the calendar on, in case of rotation
        currentShownMonth = compactCalendar.getFirstDayOfCurrentMonth();
        monthView.setText(months[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.YEAR));
        eventDay.setText(App.printDate(calendar));

        compactCalendar.setDayColumnNames(getResources().getStringArray(R.array.weekdays_short));
        compactCalendar.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {

                Calendar cal = Calendar.getInstance();
                cal.setTime(dateClicked);
                eventDay.setText(App.printDate(cal));

                bills = new ArrayList<>();

                int numberOfBills = 0;
                String printedText = "";
                for (Event e : compactCalendar.getEvents(dateClicked)) {
                    if (e.getData() instanceof String && ((String)e.getData()).substring(0, 1).equals("#")) {
                        printedText = ((String) e.getData()).substring(1) + "\n";
                    }
                    else {
                        numberOfBills++;
                        bills.add((Bill) e.getData());
                    }
                }
                if (numberOfBills > 0) {
                    String word;
                    if (numberOfBills == 1)     word = "účtenka";
                    else if (numberOfBills < 5) word = "účtenky";
                    else                        word = "účtenek";
                    printedText += numberOfBills+" "+word;
                    showBtn.setVisibility(View.VISIBLE);
                }
                else {
                    showBtn.setVisibility(View.INVISIBLE);
                }
                eventCont.setText(printedText);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                currentShownMonth = compactCalendar.getFirstDayOfCurrentMonth();
                Calendar cal = Calendar.getInstance();
                cal.setTime(firstDayOfNewMonth);
                monthView.setText(months[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR));
            }
        });

        // Add default event: next lottery draw
        calendar.setTimeInMillis(App.parseDate(DrawDate.getNextLotteryDate()).getTime());
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
        compactCalendar.addEvent(new Event(
                ContextCompat.getColor(context, R.color.billNextLotteryDate),
                calendar.getTimeInMillis(), "#Slosování účtenkovky"));

        for (Bill b : App.fetchAllBills(context)) {
            Calendar billCreationDate = b.getGivenBillDate();
            if (billCreationDate == null) continue;

            calendar.setTimeInMillis(billCreationDate.getTimeInMillis());

            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);

            compactCalendar.addEvent(new Event(
                    ContextCompat.getColor(getContext(), R.color.billIsWinning),
                    calendar.getTimeInMillis(), b));
        }

        showBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bills != null && bills.size() > 0) {
                    if (floater == null) {
                        floater = new BillsFloater(new BillsFloater.Listener() {
                            @Override
                            public void popDetail(final Bill bill) {
                                detail = new BillDetail();
                                detail.show(context, bill, new BillDetail.Listener() {
                                    @Override
                                    public void register() {
                                        Intent intent = new Intent(getActivity(), MBRActivity.class);
                                        intent.putExtra("BILL_FROM_DETAIL", bill.getRegPayload());
                                        getActivity().startActivityForResult(intent, REQUEST_BILL_REG);
                                    }

                                    @Override
                                    public void delete() {
                                        App.removeBill(context, bill);
                                    }
                                });
                            }
                        });
                    }
                    floater.show(getActivity(), bills);
                }
            }
        });
    }

    @Override
    public void onPause() {
        BillDetail.hide(detail);
        BillsFloater.hide(floater);
        if (css != null) css.set(currentShownMonth);
        super.onPause();
    }
}
