package com.andrejhucko.andrej.frontend.dialogs;

import android.view.*;
import android.widget.*;
import android.content.Context;
import android.annotation.SuppressLint;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.utility.*;

public final class AppToast {

    public static void show(Context context, Object message) throws IllegalArgumentException {

        if (context == null || message == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.toast, null);

        TextView text = layout.findViewById(R.id.ltoast_message);

        if (message.getClass() == Integer.class) {
            text.setText((int) message);
        }
        else if (message.getClass() == String.class) {
            text.setText((CharSequence) message);
        }
        else {
            throw new IllegalArgumentException();
        }

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, App.dpToPixel(context, 64));

        if (text.getText().toString().length() > 50) {
            toast.setDuration(Toast.LENGTH_LONG);
        }
        else {
            toast.setDuration(Toast.LENGTH_SHORT);
        }

        toast.setView(layout);
        toast.show();

    }

}
