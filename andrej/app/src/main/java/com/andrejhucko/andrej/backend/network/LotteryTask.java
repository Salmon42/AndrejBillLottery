package com.andrejhucko.andrej.backend.network;

import android.os.AsyncTask;
import android.content.Context;
import java.lang.ref.WeakReference;
import android.support.annotation.NonNull;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.backend.bill.BillManager;

public class LotteryTask extends AsyncTask<LotteryTask.Type, Void, Response> {

    public static final int RET_OK = 0;
    public static final int RET_BAD_TOKENS = -1;
    public static final int RET_NO_CONNECTION = -2;
    public static final int RET_MISSING_CURRENT_ACC = -3;
    public static final int RET_NO_UPDATE_NEEDED = 1;

    public enum Type { LOGIN, REG_BILL, GET_BILLS }

    public interface Listener {
        void onReceivedResult(Response response);
    }

    /** Reference to context, needs to be weak for sake of not leaking data                       */
    private WeakReference<Context> contextRef;
    /** Connection class for doing the actual job                                                 */
    private Connection connection;
    /** For getting the bill payload for registration                                             */
    private BillManager manager = null;
    /** Listener for making callback after the asyncTask                                          */
    private Listener listener = null;
    /** String containing e-mail or phone of given account                                        */
    private String user;
    /** Needs to be set with {@link LotteryTask#setLogin(LoginDetails)}                           */
    private LoginDetails login = null;

    public LotteryTask(Context context, String user) {
        contextRef = new WeakReference<>(context);
        connection = new Connection(context);
        this.user = user;
        if (user == null) {
            Reporter.send(false, "LotteryTask constructor: upcoming NullPtrEx", "passing null user");
        }
    }

    public void setLogin(@NonNull LoginDetails login) {
        this.login = login;
    }

    public void setManager(@NonNull BillManager manager) {
        this.manager = manager;
    }

    public void setListener(@NonNull Listener listener) {
        this.listener = listener;
    }

    @Override
    protected Response doInBackground(Type... types) {
        if (types.length == 0 || types.length > 1) return null;

        Context context = contextRef.get();
        App.n(context, "LotteryTask :: doInBackground");
        String accessToken = Storage.def(context).getAccToken(user);
        if (accessToken == null && types[0] != Type.LOGIN) {
            return new Response(RET_BAD_TOKENS);
        }

        if (types[0] == Type.LOGIN) {
            App.n(login, "Logging in without login details");
            return connection.getAuthTokensFromLogin(login);
        }
        else {
            // Possible types: REG_BILL, GET_BILLS, GET_ACC_SETTINGS
            Response testValidity = connection.testAuthToken(accessToken);
            if (testValidity.code() != 200) {
                // Current accessToken is invalid
                AuthTokens old = App.getAuthTokens(context, user);
                if (old == null) return new Response(RET_BAD_TOKENS);
                Response renew = connection.renewAuthTokens(user, old);
                if (renew.code() != 200) return new Response(RET_BAD_TOKENS);

                // move new access token to "auth" variable
                AuthTokens newTokens = new AuthTokens(renew.json());
                accessToken = newTokens.access();
            }

            switch (types[0]) {
                case REG_BILL: return connection.registerBill(accessToken, manager.getRegPayload());
                case GET_BILLS: return connection.getBills(accessToken, user);
                default: return new Response(RET_OK);
            }
        }
    }

    @Override
    protected void onPostExecute(Response response) {
        super.onPostExecute(response);
        if (listener != null) {
            listener.onReceivedResult(response);
        }
    }

}
