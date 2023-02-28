package com.andrejhucko.andrej.backend.utility;

import org.json.*;
import static com.andrejhucko.andrej.backend.utility.App.*;

public class LoginDetails {
    private String username;
    private String password;

    public LoginDetails(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public JSONObject json() {
        JSONObject object = new JSONObject();
        try {
            object.put(USERNAME, username);
            object.put(PASSWORD, password);
        }
        catch (JSONException ignored) {}
        return object;
    }

    @Override
    public String toString() {
        return json().toString();
    }
}
