package com.andrejhucko.andrej.backend.network;

import java.net.MalformedURLException;
import java.net.URL;

public enum Link {

    LOGIN       ("https://www.uctenkovka.cz/api/web/auth/login"),
    REFRESH     ("https://www.uctenkovka.cz/api/web/auth/refresh"),
    ADD_BILL    ("https://www.uctenkovka.cz/api/web/player/receipts/"),
    ACCOUNT     ("https://www.uctenkovka.cz/api/web/player"),
    BILL_LIST   (""),
    WIN_LIST    ("");

    private URL url;
    Link(String urlString) {
        // System.out.println("CON: " + this);
        try {
            url = new URL(urlString);
        }
        catch (MalformedURLException e) {
            url = null;
        }
    }

    /**
     * Please call this only when using for LOGIN, ADD_BILL & ACCOUNT
     * @return URL
     */
    public URL get() {
        if (this == BILL_LIST || this == WIN_LIST) {
            return get(0, 50);
        }
        return url;
    }

    /**
     * Please call this only when using for BILL_LIST & WIN_LIST
     * @param page which offset
     * @param size chunk size
     * @return URL
     */
    public URL get(int page, int size) {

        if (this == BILL_LIST) {
            try {
                url = new URL("https://www.uctenkovka.cz/api/web/player/receipts/?direction=DESC&page="
                        + Integer.toString(page) + "&size=" + Integer.toString(size) + "&sortBy=id");
            }
            catch (Exception e) { return null; } // won't happen
        }
        else if (this == WIN_LIST) {
            try {
                url = new URL("https://www.uctenkovka.cz/api/web/player/winnings/?direction=DESC&page="
                        + Integer.toString(page) + "&size=" + Integer.toString(size) + "&sortBy=id");
            }
            catch (Exception e) { return null; } // won't happen
        }
        return url;
    }
}
