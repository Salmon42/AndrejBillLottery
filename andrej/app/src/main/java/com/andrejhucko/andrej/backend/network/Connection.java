package com.andrejhucko.andrej.backend.network;

import java.io.*;
import java.net.*;
import org.json.*;
import android.content.*;
import javax.net.ssl.HttpsURLConnection;
import android.support.annotation.NonNull;
import com.crashlytics.android.Crashlytics;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.backend.bill.Status;
import static com.andrejhucko.andrej.backend.network.LotteryTask.*;

final class Connection {

    private Context context;
    Connection(@NonNull Context context) {
        this.context = context;
    }

    /**
     * "Low-level" method used by other methods in this class.
     * Sends HTTP GET/POST request with or without authorization.
     *
     * @param link URL to send the request to
     * @param auth access token, null if getting new one, or freshly logging in
     * @param payload content to be sent in the request (POST), if null, send as GET
     * @return the actual response
     */
    @NonNull
    private Response sendRequest(URL link, String auth, String payload) {
        int code = 0;
        String content = null;
        try {
            HttpsURLConnection connection = (HttpsURLConnection) link.openConnection();
            if (auth != null) {
                connection.setRequestProperty("Authorization", "Bearer " + auth);
            }
            if (payload != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestMethod("POST");

                OutputStream outputStream = connection.getOutputStream();
                BufferedWriter bufferedWriter =
                        new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

                bufferedWriter.write(payload);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();
            }
            else {
                connection.setDoOutput(false);
                connection.setRequestMethod("GET");
            }

            code = connection.getResponseCode();
            InputStream responseStream;

            if (code < HttpURLConnection.HTTP_BAD_REQUEST /* <400 */) {
                responseStream = connection.getInputStream();
            }
            else {
                responseStream = connection.getErrorStream();
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder stringBuilder = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    break;
                }
                stringBuilder.append(readLine).append('\n');
            }
            content = stringBuilder.toString();
            return new Response(code, content);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Response(code, content);
        }
    }

    /**
     * Test whether given access token is valid.
     * @param accessToken the given access token
     * @return response
     */
    @NonNull
    Response testAuthToken(@NonNull String accessToken) {
        return sendRequest(Link.BILL_LIST.get(0, 1), accessToken, null);
    }

    /**
     * Use the login details to receive auth tokens. If valid, store them in preferences
     * (accesstoken normally, refreshtoken encrypted)
     * @param details Container with username and password
     * @return response with access token & refresh token in JSON
     */
    @NonNull
    Response getAuthTokensFromLogin(@NonNull LoginDetails details) {
        Response r = sendRequest(Link.LOGIN.get(), null, details.toString());
        if (r.code() == 200) {
            App.storeAuthTokens(context, details.username(), new AuthTokens(r.json()));
        }
        return new Response(r.code());
    }

    /**
     * Use the auth tokens to get new ones - called once the previous access token is invalid
     * @param oldTokens stringified JSON with accesstoken and refreshtoken
     * @return response with new access token & refresh token in JSON
     */
    @NonNull
    Response renewAuthTokens(String user, @NonNull AuthTokens oldTokens) {

        Response r = sendRequest(Link.REFRESH.get(), null, oldTokens.toString());
        if (r.code() == 200) {
            App.storeAuthTokens(context, user, new AuthTokens(r.json()));
        }
        return r;

    }

    /**
     * Uctenkovka API - send bill registration form
     * @param auth Bearer with accesstoken
     * @param bill stringified bill JSON
     * @return response with code and content
     */
    @NonNull
    Response registerBill(@NonNull String auth, @NonNull String bill) {
        Response r = sendRequest(Link.ADD_BILL.get(), auth, bill);
        try {
            JSONObject obj = new JSONObject();

            if (r.code() == 201) {
                obj = r.json();
                obj.put(JsonKey.ID.n(), obj.get(JsonKey.R_ID.n()));
                obj.put(JsonKey.STATUS.n(), obj.get(JsonKey.R_STATUS.n()));
            }
            else if (r.code() == 400) {
                obj = (JSONObject) new JSONArray(r.content()).get(0);
                obj.put(JsonKey.ID.n(), null);
                String reason = obj.getString("code");

                String field = null;
                if (!obj.isNull("field")) field = obj.getString("field");

                switch (reason) {
                    case "object.duplicate":
                        obj.put(JsonKey.STATUS.n(), Status.DUPLICATE.n());
                        break;
                    case "object.invalid":
                        if (field != null && field.equals("date")) {
                            obj.put(JsonKey.STATUS.n(), Status.OUTDATED.n());
                        }
                        break;
                    default:
                        obj.put(JsonKey.STATUS.n(), Status.UNHANDLED.n());
                        Reporter.send(false, "Connection.registerBill: unhlandled reason", r.toString());
                        break;
                }

            }
            return new Response(r.code(), obj.toString());
        }
        catch (JSONException e) {
            Crashlytics.logException(e);
            return r;
        }
    }


    @NonNull
    Response getBills(@NonNull String auth, String user) {

        if (user == null) return new Response(RET_MISSING_CURRENT_ACC);
        BillDownloader downloader = new BillDownloader(context, user);

        try {
            // Part 1: send initial ping
            Response initialResponse = sendRequest(Link.BILL_LIST.get(0, 1), auth, null);
            boolean shouldUpdate = downloader.parseInitialResponse(initialResponse);

            if (!shouldUpdate || downloader.shouldStop())
                return new Response(RET_NO_UPDATE_NEEDED);

                // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

            // Part 2: get the update of bills!
            for (int iter = 0; iter < ((downloader.total() / 50) + 1); iter++) {
                if (downloader.shouldStop()) break;

                // 1: fetch whole data batch after we know the number of bills
                Response batch = sendRequest(Link.BILL_LIST.get(iter, 50), auth, null);
                int currentEndIndex = batch.json().getJSONArray("data").length();

                for (int i = 0; i < currentEndIndex; i++) {
                    if (downloader.shouldStop()) break;
                    JSONObject current = batch.json().getJSONArray("data").getJSONObject(i);
                    downloader.parseBill(current);
                } // oh yes
            } // I'm almost there

            // Part 3: get winnings!
            Response winnings = sendRequest(Link.WIN_LIST.get(0, 1), auth, null);
            downloader.setTotal(winnings.json().getInt("total"));
            if (downloader.total() == 0) {
                App.recountTransientBills(context, user);
                return new Response(RET_OK);
            }

            for (int iter = 0; iter < ((downloader.total() / 50) + 1); iter++) {
                Response batch = sendRequest(Link.WIN_LIST.get(iter, 50), auth, null);
                int currentEndIndex = batch.json().getJSONArray("data").length();

                for (int i = 0; i < currentEndIndex; i++) {
                    JSONObject current = batch.json().getJSONArray("data").getJSONObject(i);
                    downloader.parseWinning(current);
                }
            }
        } // aah (whole try block done)
        catch (JSONException e) {
            Crashlytics.logException(e);
        }

        App.recountTransientBills(context, user);
        return new Response(RET_OK);
    }

}
