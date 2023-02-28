package com.andrejhucko.andrej.frontend.dialogs;

import android.app.Activity;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.content.*;
import java.util.regex.Pattern;
import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.network.*;
import com.andrejhucko.andrej.backend.utility.*;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_OK;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_NO_UPDATE_NEEDED;

public final class LoginDialog extends AppDialog {

    /**
     * Listener to BaseActivity to handle UI recreation
     */
    public interface Listener {
        void onFinish(boolean success);
    }

    /** Basic e-mail regex                                                                        */
    private final Pattern pattern_email = Pattern.compile("^.+@.+[.].+$");
    /** Basic 9-digit phone regex                                                                 */
    private final Pattern pattern_phone = Pattern.compile("^\\d{9}$");
    /** password containing 8+ chars & at least 1 digit inside                                    */
    private final Pattern pattern_pass = Pattern.compile("^(?=.*\\d).{8,}$");

    /** Context for inflating view, preferences & resources                                       */
    private Activity context;
    /** {@link LoginDialog.Listener} asynchronous solution for letting parent fragment know       */
    private Listener listener;
    /** Internal state variable                                                                   */
    private boolean finished;
    /** {@link Andrej} public object storing unfinished data written by user into login dialog    */
    private LoginDlgStorage lds;


    public LoginDialog(Activity context, LoginDlgStorage lds, Listener listener) {
        this.lds = lds;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Kinda obvious
     */
    @SuppressLint("InflateParams")
    public void show() {
        if (isOnScreen) return; // Must-be as first
        finished = false;

        LayoutInflater inflater = LayoutInflater.from(context);
        // final View loginDialogView = inflater.inflate(R.layout.dialog__login, null);
        dialogView.set(inflater.inflate(R.layout.dialog__login, null));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppRoundDialog);
        alertDialog.set(builder.setView(dialogView.get()).create());

        // If this dialog is to continue the previous instance, refill the edit texts
        reloadFromPreviousDialog(dialogView.get());

        final Switch choiceSwitch = dialogView.get().findViewById(R.id.ld_switch);
        choiceSwitch.setOnCheckedChangeListener(switchListenerFactory(dialogView.get()));
        choiceSwitch.setChecked(true);

        dialogView.get().findViewById(R.id.ld_button_negative).setOnClickListener(negativeBtnListenerFactory());
        dialogView.get().findViewById(R.id.ld_button_positive).setOnClickListener(positiveBtnListenerFactory(dialogView.get()));
        alertDialog.get().setOnCancelListener(cancelListenerFactory());
        alertDialog.get().setCanceledOnTouchOutside(false);
        super.show();
    }


    /**
     * Throws toast if any of the credentials are input wrong.
     * @param choice   e-mail | phone ; for decision
     * @param email    -
     * @param phone    -
     * @param password -
     * @return data with email or phone according to current switch choice
     */
    private String checkCredentials(String choice, String email, String phone, String password) {

        String chosenInput;

        if (choice.equals(context.getString(R.string.email))) { // "E-mail"
            chosenInput = email;
            if (!pattern_email.matcher(chosenInput).find()) {
                AppToast.show(context, R.string.dlg__login_email_err);
                return null;
            }
        }
        else {
            chosenInput = phone;
            if (!pattern_phone.matcher(chosenInput).find()) {
                AppToast.show(context, R.string.dlg__login_phone_err);
                return null;
            }
        }

        if (!pattern_pass.matcher(password).find()) {
            AppToast.show(context, R.string.dlg__login_pass_err);
            return null;
        }

        return chosenInput;
    }


    /**
     * Called when parent (fragment) gets removed from screen.
     * Possible situations:
     *      ~ Screen rotation
     *      ~ Home button
     *      ~ Apps button (app manager to force GC)
     */
    public void onParentDestroyed() {
        if (!finished) {
            lds.shouldReappear(true);
            lds.setData(new String[] {
                ((TextView) dialogView.get().findViewById(R.id.ld_email)).getText().toString(),
                ((TextView) dialogView.get().findViewById(R.id.ld_phone)).getText().toString(),
                ((TextView) dialogView.get().findViewById(R.id.ld_pass)).getText().toString()
            });
        }
        else {
            lds.clear();
        }
        super.onParentDestroyed();
    }


    /**
     * If the Application class holds unfinished credentials, recreate the dialog and put it there
     * @param loginDialogView view to find editTexts
     */
    private void reloadFromPreviousDialog(final View loginDialogView) {
        // Is this logindialog a continued work from before?
        if (lds.shouldReappear()) {
            // Do not call any other index than 0, 1 or 2
            ((EditText) loginDialogView.findViewById(R.id.ld_email)).setText(lds.getData(0));
            ((EditText) loginDialogView.findViewById(R.id.ld_phone)).setText(lds.getData(1));
            ((EditText) loginDialogView.findViewById(R.id.ld_pass)).setText(lds.getData(2));
            lds.clear();
        }
    }


    private CompoundButton.OnCheckedChangeListener switchListenerFactory(final View loginDialogView) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                TextView tv = loginDialogView.findViewById(R.id.ld_choice);
                EditText mailEdit = loginDialogView.findViewById(R.id.ld_email);
                EditText phoneEdit = loginDialogView.findViewById(R.id.ld_phone);

                if (b) {
                    tv.setText(R.string.email);
                    phoneEdit.setVisibility(View.INVISIBLE);
                    mailEdit.setVisibility(View.VISIBLE);
                    mailEdit.requestFocus();
                }
                else {
                    tv.setText(R.string.phone);
                    mailEdit.setVisibility(View.INVISIBLE);
                    phoneEdit.setVisibility(View.VISIBLE);
                    phoneEdit.requestFocus();
                }
            }
        };
    }


    private View.OnClickListener positiveBtnListenerFactory(final View loginDialogView) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // check again the internet connection (done before instantiating this class)
                if (!App.isConnected(context)) {
                    AppToast.show(context, R.string.net_err_toast);
                    return;
                }

                TextView currentChoice = loginDialogView.findViewById(R.id.ld_choice);

                EditText emailEdit = loginDialogView.findViewById(R.id.ld_email);
                EditText phoneEdit = loginDialogView.findViewById(R.id.ld_phone);
                EditText passEdit = loginDialogView.findViewById(R.id.ld_pass);

                final String inputEmail = emailEdit.getText().toString();
                final String inputPhone = phoneEdit.getText().toString();
                final String inputPass = passEdit.getText().toString();
                final String choice = currentChoice.getText().toString();
                final String chosenInput = checkCredentials(choice, inputEmail, inputPhone, inputPass);
                if (chosenInput == null) return;

                LotteryTask loginTask = new LotteryTask(context, chosenInput);
                loginTask.setLogin(new LoginDetails(chosenInput, inputPass));
                loginTask.setListener(new LotteryTask.Listener() {
                    @Override
                    public void onReceivedResult(Response response) {
                        if (response.code() == 200) {

                            Storage.def(context).addAccount(chosenInput, choice);
                            NetworkFetch.getDataWithDialog(context, new NetworkFetch.Listener() {
                                @Override
                                public void onFinish(Response onFinishResponse) {

                                    switch(onFinishResponse.code()) {

                                        case RET_OK:
                                        case RET_NO_UPDATE_NEEDED:
                                            AppToast.show(context, context.getString(R.string.toast__login_succ));
                                            break;

                                        default:
                                            AppToast.show(context, context.getString(R.string.toast__login_fail));
                                    }

                                    DefStorage store = Storage.def(context);
                                    store.setValidUser(store.getCurrentAcc());
                                    lds.clear();
                                    finished = true;
                                    listener.onFinish(true);
                                    alertDialog.get().dismiss();
                                }
                            });
                        }
                        else {
                            AppToast.show(context, R.string.toast__login_alert_fail);
                        }
                    }
                });
                loginTask.execute(LotteryTask.Type.LOGIN);

            }
        };
    }


    private View.OnClickListener negativeBtnListenerFactory() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lds.clear();
                finished = true;
                listener.onFinish(true);
                alertDialog.get().dismiss();

                // Don't open any browser if there's no internet connection
                if (!App.isConnected(context)) {
                    AppToast.show(context, R.string.net_err_gen);
                }
                else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.uctenkovka_reglink)));
                    context.startActivity(browserIntent);
                }
            }
        };
    }


    private DialogInterface.OnCancelListener cancelListenerFactory() {
        return new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                lds.clear();
                finished = true;
                listener.onFinish(false);
                dialog.dismiss();
            }
        };
    }


    public static void destroy(LoginDialog dialog) {
        if (dialog != null) {
            dialog.onParentDestroyed();
        }
    }

}
