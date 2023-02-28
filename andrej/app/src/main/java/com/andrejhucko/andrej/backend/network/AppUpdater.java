package com.andrejhucko.andrej.backend.network;

import android.content.Context;
import com.google.firebase.database.*;
import com.andrejhucko.andrej.BuildConfig;
import android.support.annotation.NonNull;
import com.andrejhucko.andrej.backend.utility.App;

public class AppUpdater {

    public interface Listener {
        void afterCheck(boolean shouldUpdate);
    }

    /**
     * Check for update
     * @param doublePrevention - prevent from calling this twice (two baseactivities corruption)
     * @param context          - for internet connection
     * @param listener         - for caller what to do then
     * @return whether the code try to check for update
     */
    public static boolean checkForUpdate(boolean doublePrevention, final Context context, final Listener listener) {
        if (!doublePrevention) {
            return false;
        }

        if (!App.isConnected(context)) {
            listener.afterCheck(false);
            return false;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("version");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Long value = (Long) dataSnapshot.getValue();
                if (value == null) {
                    listener.afterCheck(false);
                }
                else {
                    // value == 9, installed app == 8 -> update!
                    listener.afterCheck(value > BuildConfig.VERSION_CODE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.afterCheck(false);
            }
        });
        return true;
    }

}
