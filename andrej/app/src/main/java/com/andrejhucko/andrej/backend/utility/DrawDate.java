package com.andrejhucko.andrej.backend.utility;

import java.util.Calendar;
import android.content.Context;
import com.andrejhucko.andrej.R;

/** Just cute wrapper for little functionality */
public class DrawDate {

    private int y, m, d;

    public DrawDate(String date) {

        if (date.length() != 10)
            throw new IllegalArgumentException();

        y = Integer.valueOf(date.substring(0, 4));
        m = Integer.valueOf(date.substring(5, 7));
        d = Integer.valueOf(date.substring(8, 10));

    }

    public String get() {
        String sm = (m<10) ? "0" + m : Integer.toString(m);
        String sd = (d<10) ? "0" + d : Integer.toString(d);
        return y + "-" + sm + "-" + sd;
    }

    /** Add month to the draw date */
    public DrawDate add() {

        m++;
        if (m > 12) {
            m = 1;
            y++;
        }
        return this;

    }

    /** Subtract month from the draw date */
    public DrawDate sub() {

        m--;
        if (m < 1) {
            m = 12;
            y--;
        }
        return this;

    }

    /**
     * Generate date with next lottery drawing date (according to some rules)
     * @return date with format YYYY-MM-DD
     */
    public static String getNextLotteryDate() {

        Calendar cal = Calendar.getInstance();
        int yearVal = cal.get(Calendar.YEAR),
                monthVal = cal.get(Calendar.MONTH) + 1,
                dayVal = cal.get(Calendar.DAY_OF_MONTH);

        if (dayVal > 15) {
            monthVal++;
        }
        dayVal = 15;

        if (monthVal > 12) {
            monthVal = 1;
            yearVal++;
        }

        String year = String.valueOf(yearVal);
        String month = String.valueOf((monthVal < 10) ? "0"+monthVal : monthVal);
        String day = String.valueOf(dayVal);

        return year + "-" + month + "-" + day;
    }

    public static String print(Context context, DrawDate date) {

        String[] months = context.getResources().getStringArray(R.array.months);
        return months[date.m - 1] + " " + date.y;

    }

}
