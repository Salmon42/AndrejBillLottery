package com.andrejhucko.andrej.frontend;

import android.text.*;
import android.view.*;
import android.graphics.Color;
import android.widget.TextView;
import android.support.v7.app.AlertDialog;
import android.support.annotation.CallSuper;
import android.graphics.drawable.ColorDrawable;
import com.andrejhucko.andrej.frontend.dialogs.AppDialogInterface;

public class AppDialog implements AppDialogInterface {

    private static final int SIZE = WindowManager.LayoutParams.WRAP_CONTENT;

    /** Instance variable to {@link AlertDialog} object with lazy initialization                  */
    protected final LazyAlertDialog alertDialog;
    /** Instance vatiable to {@link LazyView} object with lazy init                         */
    protected final LazyView dialogView;
    /** Internal state variable                                                                   */
    protected boolean isOnScreen;

    private int titleId = 0;
    private String title = null;
    private int messageId = 0;
    private String message = null;
    private Spanned messageHtml = null;

    private int viewTitleId = 0;
    private int viewMessageId = 0;

    protected AppDialog() {
        alertDialog = new LazyAlertDialog();
        dialogView = new LazyView();
    }

    /**
     * Set title and message content.
     * @param vti     Layout view of title inside the dialog
     * @param vmi     Layout view of message inside the dialog
     * @param title   Resource ID or plain String
     * @param message Resource ID or plain String
     */
    protected AppDialog(int vti, int vmi, Object title, Object message) {
        alertDialog = new LazyAlertDialog();
        dialogView = new LazyView();
        isOnScreen = false;

        viewTitleId = vti;
        viewMessageId = vmi;

        if (title.getClass() == Integer.class) {
            this.titleId = (int) title;
        }
        else if (message.getClass() == String.class) {
            this.title = (String) title;
        }
        else {
            throw new IllegalArgumentException("Title with wrong class type.");
        }

        if (message.getClass() == Integer.class) {
            this.messageId = (int) message;
        }
        else if (message.getClass() == String.class) {
            this.message = (String) message;
        }
        else if (message.getClass() == SpannableStringBuilder.class) {
            this.messageHtml = (Spanned) message;
        }
        else {
            throw new IllegalArgumentException("Message with wrong class type.");
        }

    }

    /** Set title by resource ID */
    public AppDialog setTitle(int titleId) {
        this.titleId = titleId;
        return this;
    }

    /** Set title by String */
    public AppDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    /** Set message by resource ID */
    public AppDialog setMessage(int messageId) {
        this.messageId = messageId;
        return this;
    }

    /** Set message by String */
    public AppDialog setMessage(String message) {
        this.message = message;
        return this;
    }


    protected void prepareContent() {

        TextView titleView = dialogView.get().findViewById(viewTitleId);
        TextView msgView = dialogView.get().findViewById(viewMessageId);
        if (title != null) titleView.setText(title); else titleView.setText(titleId);

        if (message != null) {
            msgView.setText(message);
        }
        else if (messageId != 0) {
            msgView.setText(messageId);
        }
        else {
            msgView.setText(messageHtml);
        }

    }

    @Override
    @CallSuper
    public void show() {
        Window wnd = alertDialog.get().getWindow();
        if (wnd != null) {
            wnd.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            wnd.setLayout(SIZE, SIZE);
        }
        alertDialog.get().show();
        isOnScreen = true;
    }

    protected boolean mayShow() {
        if (isOnScreen) return false;
        return (titleId != 0 || title != null) && (messageId != 0 || message != null || messageHtml != null);
    }


    @Override
    @CallSuper
    public void onParentDestroyed() {
        alertDialog.dismiss();
        isOnScreen = false;
    }

    public static void destroy(AppDialog... dialogs) {
        if (dialogs.length == 0) return;
        for (AppDialog dlg : dialogs) {
            if (dlg != null) {
                dlg.onParentDestroyed();
            }
        }
    }

}
