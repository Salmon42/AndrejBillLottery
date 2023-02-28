package com.andrejhucko.andrej.backend.network;

import org.json.*;
import java.util.*;
import android.content.Context;
import android.content.SharedPreferences;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.utility.*;
import com.crashlytics.android.Crashlytics;

/** Basically used as a singleton for Connection.getBills */
class BillDownloader {

    private Context context;
    private DefStorage def;
    private SharedPreferences ub, rbptr;

    private String user;
    private Set<String> regBillDraws;

    private int total;
    private int storedTBC;
    private String storedBLF;
    private boolean mayStopFetching;

    BillDownloader(Context context, String user) {
        this.context = context;

        rbptr = null;
        def = Storage.def(context);
        ub = Storage.ub(context);

        this.user = user;
        storedTBC = def.getTbc(user);
        storedBLF = def.getBlf(user);
        total = 0;
    }

    void setTotal(int total) {
        this.total = total;
    }

    int total() {
        return total;
    }

    /**
     * Should download stop?
     * mayStopFetching ->   the fetching timestamp (of latest bill) is the same as before,
     *                      this means the only thing that can be updated are transient bill states
     *
     * storedTBC ->         counter for stored transient bills (which can change state)
     *                      if this reaches zero, there is no need to continue
     */
    boolean shouldStop() {
        return mayStopFetching && storedTBC == 0;
    }

    boolean parseInitialResponse(Response r) {
        try {
            total = r.json().getInt("total");
            if (total == 0) return false;

            // JSON of most recent bill (mrb) & fetching player ID + time of registration of this bill
            JSONObject mrb = r.json().getJSONArray("data").getJSONObject(0);
            // bill registration timestamp for MOST RECENT bill (for the user)
            String fetchedRegTime = mrb.getString(JsonKey.REG_TS.n()); //"registrationDateTime"
            // The player ID (required later for matching with bills from preferences("bills_to_send")
            Integer fetchedPlayerID = mrb.getInt(JsonKey.PLAYER.n()); //"playerId"

            // check for storing player's generated ID in default prefs ; PID = Player ID
            if (def.getPid(user) == -1) def.setPid(user, fetchedPlayerID);

            if (storedBLF == null) {
                // Store the new TS (most-up-to-date-registration)
                def.setBlf(user, fetchedRegTime);
            }
            else {
                Date fetched = App.parseDate(fetchedRegTime); // fetched right now
                Date stored = App.parseDate(storedBLF); // found in preferences

                if (stored.compareTo(fetched) < 0) { // fetched is later than stored
                    def.setBlf(user, fetchedRegTime);
                }
                else {
                    mayStopFetching = true;
                    // Situation: stored TS is the same as online last reg. TS.
                    // No bills with NEW | IN_DRAW | VERIFIED that need to be updated?
                    // well why sending another requests. end here right now
                    if (storedTBC == 0) return false;
                }
            }

            regBillDraws = def.getDraws(user);

        }
        catch (JSONException e) {
            Crashlytics.logException(e);
        }

        return true;
    }

    void parseBill(JSONObject currentBill) {
        try {
            if (Status.get(currentBill.getString(JsonKey.STATUS.n())) == Status.REJECTED) {
                return; // Do not store any "rejected" bills
            }

            // Should match with a unsent bill registry?
            if (ub.getAll().size() > 0) {
                // storage contains "unsent" bills, try to pair them to registered ones
                // if found, then extract data to the downloaded json and remove the old one

                // get BKP & FIK code from the current JSONObject to search for in the sentBillsPreferences
                String cFIK = (String) Bill.fromJSON(currentBill, JsonKey.FIK);
                if (cFIK != null) cFIK = cFIK.toLowerCase();

                String cBKP = (String) Bill.fromJSON(currentBill, JsonKey.BKP);
                if (cBKP != null) cBKP = cBKP.substring(0, 17);

                String storedBillJSONString = null;
                String currentSearch = null;

                // Try to fetch bill JSON from sent-bills storage.
                // Required to search by FIK & BKP. Tested once with FIK, then with BKP.
                if (ub.contains(cFIK)) {
                    storedBillJSONString = ub.getString(cFIK, null);
                    currentSearch = cFIK;
                }
                else if (ub.contains(cBKP)) {
                    storedBillJSONString = ub.getString(cBKP, null);
                    currentSearch = cBKP;
                }

                if (storedBillJSONString != null) {
                    // Copy DIC (vatID) from sent-bills storage to received-bills storage
                    // If nothing, null is copied, doesn't hurt
                    Bill sentBill = new Bill(storedBillJSONString);
                    currentBill.put(JsonKey.VAT_ID.n(), sentBill.getVatID());
                    // and remove it
                    Storage.remove(ub, currentSearch);
                }
            }

            // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

            // findBillByID, not stored? store with key { id : rest of json }
            // try to find the current JSON bill by ID in preferences

            String id = String.valueOf(currentBill.getInt(JsonKey.ID.n()));
            String drawDate = currentBill.getString(JsonKey.DRAW_DATE.n());

            rbptr = Storage.rb(context, drawDate);
            boolean shouldStoreDrawSet = regBillDraws.add(drawDate);

            String storedBillStr = rbptr.getString(id, null);
            if (storedBillStr == null) {
                // did not exist in preferences, store instantly
                rbptr.edit().putString(id, currentBill.toString()).apply();
            }
            else {
                // downloaded bill JSON exists in storage, check for status update
                JSONObject storedBill = new JSONObject(storedBillStr);

                String newStatusStr = currentBill.getString(JsonKey.STATUS.n());
                String newRStatusStr = currentBill.getString(JsonKey.R_STATUS.n());
                Status n = Status.get(newRStatusStr);
                Status o = Status.get(storedBill.getString(JsonKey.STATUS.n()));

                if (n != o) {
                    if (o == Status.NEW || o == Status.VERIFIED || o == Status.IN_DRAW) {
                        storedTBC--;
                    }
                    // update status
                    storedBill.put(JsonKey.R_STATUS.n(), newRStatusStr);
                    storedBill.put(JsonKey.STATUS.n(), newStatusStr);
                    rbptr.edit().putString(id, storedBill.toString()).apply();
                }
            }

            if (shouldStoreDrawSet) {
                Storage.def(context).setDraws(user, regBillDraws);
            }

        } catch (JSONException e) {
            Crashlytics.logException(e);
        }
    }

    void parseWinning(JSONObject currentBill) {
        try {
            String id = String.valueOf(currentBill.getInt(JsonKey.R_ID.n()));
            String drawDate = currentBill.getString(JsonKey.DRAW_DATE.n());
            rbptr = Storage.rb(context, drawDate);

            String storedBillStr = rbptr.getString(id, null);
            if (storedBillStr == null)
                return;

            JSONObject storedBill = new JSONObject(storedBillStr);
            String wonprize = currentBill.getString(JsonKey.W_PRIZE.n());
            String winstate = currentBill.getString(JsonKey.W_PAYSTAT.n());

            storedBill.put(JsonKey.W_PRIZE.n(), wonprize);
            storedBill.put(JsonKey.W_PAYSTAT.n(), winstate);

            rbptr.edit().putString(id, storedBill.toString()).apply();
        }
        catch (JSONException e) {
            Crashlytics.logException(e);
        }
    }

}
