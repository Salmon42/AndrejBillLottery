package com.andrejhucko.andrej.backend.utility;

/** https://stackoverflow.com/questions/1754315/how-to-create-custom-exceptions-in-java */
public class Report extends Exception {
    public Report(String message) {
        super(message);
    }
}
