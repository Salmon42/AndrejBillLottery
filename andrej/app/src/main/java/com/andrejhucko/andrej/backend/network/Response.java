package com.andrejhucko.andrej.backend.network;

import org.json.*;

/**
 * Container for handling return values of sending and receiving data over network.
 */
public class Response {
    private int responseCode;
    private String responseContent;

    public Response(int code) {
        this.responseCode = code;
        this.responseContent = "";
    }

    Response(int code, String content) {
        this.responseCode = code;
        this.responseContent = content;
        if (content == null) {
            this.responseContent = "";
        }
    }

    public int code() {
        return responseCode;
    }

    public String content() {
        return responseContent;
    }

    public JSONObject json() {
        try {
            return new JSONObject(responseContent);
        }
        catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Response code: [" + responseCode + "]\n" + responseContent;
    }
}