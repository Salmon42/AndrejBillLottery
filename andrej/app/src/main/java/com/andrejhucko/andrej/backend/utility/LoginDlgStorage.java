package com.andrejhucko.andrej.backend.utility;

/**
 * Data class for storing text from login dialog
 * in case of shutting down the fragment.
 */
public class LoginDlgStorage {

    /**
     * [0] -> e-mail
     * [1] -> phone
     * [2] -> password
     */
    private String[] data = {"", "", ""};
    private boolean shouldReappear = false;

    public void setData(String[] data) {
        this.data = data;
    }

    public void shouldReappear(boolean value) {
        shouldReappear = value;
    }

    /** Only for [0], [1], [2] */
    public String getData(int index) {
        return data[index];
    }

    public boolean shouldReappear() {
        return shouldReappear;
    }

    /** Reset the data, and it should not appear any more */
    public void clear() {
        shouldReappear = false;
        data = new String[]{"", "", ""};
    }

}
