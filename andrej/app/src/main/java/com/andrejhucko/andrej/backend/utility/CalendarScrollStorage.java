package com.andrejhucko.andrej.backend.utility;

import java.util.Date;

/** Class storing information for LotteryFragment -
 *  which month is currently displayed.
 */
public class CalendarScrollStorage {

    private Date date = null;
    private boolean isSet = false;

    public void set(Date date) {
        this.date = date;
        this.isSet = true;
    }

    public boolean isSet() {
        return isSet;
    }

    /** Once getter is used, make it look like it's cleared */
    public Date get() {
        isSet = false;
        return date;
    }

}
