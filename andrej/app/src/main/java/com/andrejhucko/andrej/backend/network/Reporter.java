package com.andrejhucko.andrej.backend.network;

import com.crashlytics.android.Crashlytics;
import com.andrejhucko.andrej.backend.utility.*;

/** Crashlytics class for sending forced exceptions - as bug reports */
public class Reporter {

    public static void send(boolean userTriggered, String type, String message) {
        Crashlytics.setString("typeOfBug", type);
        Crashlytics.setString("reportMessage", message);
        try {
            String title;
            if (userTriggered) title = App.currentTimestamp();
            else title = "[ANDREJ DEVEL MODE]: " + App.currentTimestamp();
            throw new Report(title);
        }
        catch (Report r) {
            Crashlytics.logException(r);
        }
    }

}
