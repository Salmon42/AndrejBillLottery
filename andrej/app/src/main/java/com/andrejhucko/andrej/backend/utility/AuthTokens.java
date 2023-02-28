package com.andrejhucko.andrej.backend.utility;

import org.json.*;
import com.crashlytics.android.Crashlytics;
import static com.andrejhucko.andrej.backend.utility.App.*;

/** Wrapping class just for the pair - accessToken & refreshToken */
public class AuthTokens {

    private String accessToken;
    private String refreshToken;

    public AuthTokens() {
        accessToken = "";
        refreshToken = "";
    }

    public AuthTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public AuthTokens(JSONObject object) {
        this();
        try {
            if (object.has(ACCESS_TOKEN)) accessToken = object.getString(ACCESS_TOKEN);
            if (object.has(REFRESH_TOKEN)) refreshToken = object.getString(REFRESH_TOKEN);
        }
        catch (JSONException e) {
            Crashlytics.setString("Token", object.toString());
            Crashlytics.logException(e);
        }
    }

    public String access() {
        return accessToken;
    }

    public String refresh() {
        return refreshToken;
    }

    public JSONObject json() {
        JSONObject object = new JSONObject();
        try {
            object.put(ACCESS_TOKEN, accessToken);
            object.put(REFRESH_TOKEN, refreshToken);
        }
        catch (JSONException ignored) {}
        return object;
    }

    @Override
    public String toString() {
        return json().toString();
    }

}
