package com.andrejhucko.andrej.backend.bill;

import org.json.*;
import java.util.*;
import android.util.Log;
import android.support.annotation.NonNull;

import com.andrejhucko.andrej.backend.network.Reporter;
import com.andrejhucko.andrej.backend.utility.App;
import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.services.common.Crash;

public final class Bill implements Comparable<Bill> {

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Uctenkovka-generated attributes
    /// Uctenkovka JSON contains 12 attributes (found here & in DOUBLE-USAGE)

    /** false if standard mode, true if simple mode                 ||| "simpleMode":false,       */
    private Boolean uSimpleMode = null;
    /** sum in CZK*100 -> v halieroch                               ||| "amount":18400,           */
    private Integer uAmount = null;
    /** Format: HH:MM:SS                                            ||| "time":"20:58:00",        */
    private String uTime = null;
    /** Format: YYYY-MM-DD                                          ||| "date":"2018-01-24",      */
    private String uDate = null;

    /** for each bill 8-digit number                                ||| "id":61835270,            */
    private Integer uBillID = null;
    /** player has 6-digit number                                   ||| "playerId":785823,        */
    private Integer uPlayerID = null;
    /** fully defined in Status.java                                ||| "status":"NOT_WINNING"    */
    private String uBillStatus = null;
    /** what lottery date is this bill valid for)                   ||| "drawDate":"2018-02-15",  */
    private String uBillDrawDate = null;
    /** used as timestamp               ||| "registrationDateTime":"2018-02-01T10:50:31.58+01:00" */
    private String uRegistrationTimeStamp = null;

    /// Uctenkovka JSON - winning part, keeping 2 attributes

    /** String containing winning prize (mostly should be X Kč      ||| "winDescription":"100 Kč" */
    private String wAmount = null;
    /** Uctenkovka-internal constant for status             ||| "winPaymentStatus":"BACC_SUCCESS" */
    private String wStatus = null;

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Double-usage attributes (UI+net)

    /** DOUBLE-USAGE: DIČ (vat id)      ||| "vatId":"CZ9208286109",                               */
    private String dVatID = null;
    /** Format: 12345678-1234-4321      ||| "fik":"5194cc99-27bd-4c66",                           */
    private String dFIK = null;
    /** Format: 12345678-87654321       ||| "bkp":"f181981f-ceb222aa-78ea7bc6-302a4419-73ebe845", */
    private String dBKP = null;

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  My own attributes - for UI
    /** Should the {@link Bill#compareTo(Bill)} compare by dates of shopping or registration?     */
    private Boolean sortByRegTimeStamp = null;
    /** {@link Bill#uRegistrationTimeStamp} */
    private Date mRegistrationTimeStamp = null;
    /** {@link Bill#uAmount} */
    private float mAmount = 0.0f;

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Helper attributes
    private static final Map<Entry, HashSet> errLists;
    static {
        errLists = new HashMap<>();
        errLists.put(Entry.FIK, new HashSet<String>());
        errLists.put(Entry.BKP, new HashSet<String>());
        errLists.put(Entry.SUM, new HashSet<Integer>());
        errLists.put(Entry.TIME, new HashSet<String>());
        errLists.put(Entry.DATE, new HashSet<String>());
        errLists.put(Entry.DIC, new HashSet<String>());
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Methods
    public Bill() {}

    public Bill(String JSONString) {
        if (JSONString == null) {
            Crashlytics.log("Bill.Bill(String) :: JSONString is null");
            return;
        }

        try {
            JSONObject o = new JSONObject(JSONString);

            uBillID = (Integer) fromJSON(o, JsonKey.ID);
            uPlayerID = (Integer) fromJSON(o, JsonKey.PLAYER);
            uBillStatus = (String) fromJSON(o, JsonKey.STATUS);

            if (uBillStatus == null) uBillStatus = (String) fromJSON(o, JsonKey.R_STATUS);

            uBillDrawDate = (String) fromJSON(o, JsonKey.DRAW_DATE);
            uRegistrationTimeStamp = (String) fromJSON(o, JsonKey.REG_TS);
            uTime = (String) fromJSON(o, JsonKey.TIME);
            uDate = (String) fromJSON(o, JsonKey.DATE);
            uAmount = (Integer) fromJSON(o, JsonKey.AMOUNT);
            uSimpleMode = (Boolean) fromJSON(o, JsonKey.MODE);

            dVatID = (String) fromJSON(o, JsonKey.VAT_ID);
            dFIK = (String) fromJSON(o, JsonKey.FIK);
            dBKP = (String) fromJSON(o, JsonKey.BKP);

            wAmount = (String) fromJSON(o, JsonKey.W_PRIZE);
            wStatus = (String) fromJSON(o, JsonKey.W_PAYSTAT);

            mRegistrationTimeStamp = App.parseDate(uRegistrationTimeStamp);
            if (uAmount != null) mAmount = uAmount / 100;

        }
        catch (JSONException e) {
            Crashlytics.setString("Classmethod", "Bill.Bill(String)");
            Crashlytics.logException(e);
        }
    }

    /**
     * Helper for parametrized constructor {@link Bill#Bill(String)}
     * @param object JSONObject to retrieve data from
     * @param key    key to data in JSON
     * @return Integer, Boolean or String, cast it yourself
     */
    public static Object fromJSON(JSONObject object, JsonKey key) {
        try {
            return (object.has(key.n()))
                    ? object.get(key.n())
                    : null;
        }
        catch (JSONException e) {
            Crashlytics.setString("Classmethod", "Bill.fromJSON(JSONObject, JsonKey");
            Crashlytics.setString("Key", key.n());
            Crashlytics.setString("JSON", object.toString());
            Crashlytics.logException(e);
            return null;
        }
    }

    /**
     * Get registration JSON string to be stored inside SharedPreferences
     * @return JSONObject with filled registration information
     */
    public String getRegPayload() {
        JSONObject o = new JSONObject();
        try {
            o.put(JsonKey.AMOUNT.n(), uAmount)
             .put(JsonKey.DATE.n(), uDate)
             .put(JsonKey.TIME.n(), uTime)
             .put(JsonKey.MODE.n(), uSimpleMode)
             .put(JsonKey.PHONE.n(), null);

            if (dFIK != null) o.put(JsonKey.FIK.n(), dFIK);
            else if (dBKP != null) o.put(JsonKey.BKP.n(), dBKP);
            else return null;
        }
        catch (JSONException e) {
            Crashlytics.setString("Classmethod", "Bill.getRegPayload");
            Crashlytics.logException(e);
            return null;
        }
        return o.toString();
    }

    public void setComparationFilter(boolean sortByRegTimeStamp) {
        this.sortByRegTimeStamp = sortByRegTimeStamp;
    }

    /// UI getters
    @NonNull public String dispSimpleMode() {
        if (uSimpleMode == null) return "";
        return uSimpleMode ? "Zjednodušený" : "Běžný";
    }
    @NonNull public String dispAmount() {
        return Float.toString(mAmount) + " Kč";
    }
    @NonNull public String dispDate() {
        return App.printDate(uDate); // Format: YYYY-MM-DD
    }
    public String dispBKP() {
        if (dBKP == null) return null;
        return (dBKP.length() > 17) ? dBKP.substring(0, 17) : dBKP;
    }
    @NonNull public String dispRegDate() {
        return App.printDate(uRegistrationTimeStamp.substring(0, uRegistrationTimeStamp.indexOf("T")));
    }
    @NonNull public String dispRegTime() {
        if (uRegistrationTimeStamp == null) return "";
        return uRegistrationTimeStamp.substring(
                uRegistrationTimeStamp.indexOf("T") + 1,
                uRegistrationTimeStamp.indexOf("T") + 6
        );
    }
    @NonNull public String dispBillDrawDate() {
        return App.printDate(uBillDrawDate);
    }

    public Date getRegTimeStamp() {
        return mRegistrationTimeStamp;
    }
    public Calendar getGivenBillDate() {

        if (uDate == null || uTime == null) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(App.parseDate(uDate).getTime());

        int hour = Integer.valueOf(uTime.substring(0, 2));
        int min = Integer.valueOf(uTime.substring(3, 5));

        cal.set(Calendar.HOUR, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 1);

        return cal;
    }

    // Extended getter for preferences storage
    String getBillStorageString() {
        JSONObject obj = new JSONObject();
        try {
            obj.put(JsonKey.FIK.n(), dFIK)
               .put(JsonKey.BKP.n(), dBKP)
               .put(JsonKey.AMOUNT.n(), uAmount)
               .put(JsonKey.DATE.n(), uDate)
               .put(JsonKey.TIME.n(), uTime)
               .put(JsonKey.MODE.n(), uSimpleMode)
               .put(JsonKey.VAT_ID.n(), dVatID)
               .put(JsonKey.R_STATUS.n(), "NOT_REGISTERED")
               .put(JsonKey.REG_TS.n(), App.currentTimestamp());
        }
        catch (JSONException e) {
            Crashlytics.logException(e);
            return null;
        }
        return obj.toString();
    }

    /// Raw getters
    public Boolean getSimpleMode() { return uSimpleMode; }
    public Integer getAmount() { return uAmount; }
    public Float getFloatAmount() { return mAmount; }
    public String getTime() { return uTime; }
    public String getDate() { return uDate; }
    public Integer getBillID() { return uBillID; }
    public Integer getPlayerID() { return uPlayerID; }
    public Status getStatus() { return Status.get(uBillStatus); }
    public String getBillDrawDate() { return uBillDrawDate; }
    public String getStringRegTimeStamp() { return uRegistrationTimeStamp; }
    public String getVatID() { return dVatID; }
    public String getFIK() { return dFIK; }
    public String getBKP() { return dBKP; }
    public String getWinPrize() { return wAmount; }
    public String getWinStatus() { return wStatus; }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Error list methods

    /**
     * Add current-stored item to error list
     * @param entry type of bill item
     */
    @SuppressWarnings("unchecked")
    void addToErrList(Entry entry) {
        switch (entry) {
            case FIK:   errLists.get(entry).add(dFIK);      return;
            case BKP:   errLists.get(entry).add(dBKP);      return;
            case SUM:   errLists.get(entry).add(uAmount);   return;
            case DATE:  errLists.get(entry).add(uDate);     return;
            case TIME:  errLists.get(entry).add(uTime);     return;
            case DIC:   errLists.get(entry).add(dVatID);
        }
    }

    /**
     * Is the object in given error list
     * @param entry {@link Entry} type
     * @param item  String / Integer, cast it yourself pls
     * @return quite selfdocumenting
     */
    boolean isInErrList(Entry entry, Object item) {
        Set list = errLists.get(entry);
        return (list != null) && list.contains(item);
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Generics getter & setter

    void setEntry(Entry entry, Object data) {
        switch (entry) {
            case FIK:
                this.dFIK = ((String) data).toLowerCase();
            return;
            case BKP:
                this.dBKP = ((String) data).toLowerCase();
            return;
            case TIME:
                this.uTime = (String) data;
            return;
            case DATE:
                this.uDate = (String) data;
            return;
            case MODE:
                this.uSimpleMode = (Boolean) data;
            return;
            case DIC:
                this.dVatID = ((String) data).toUpperCase();
            return;
            case SUM:
                this.uAmount = ((Integer) data);
                this.mAmount = (float) uAmount / 100;
        }
    }

    void removeEntry(Entry entry) {
        switch (entry) {
            case FIK:   this.dFIK = null;           return;
            case BKP:   this.dBKP = null;           return;
            case TIME:  this.uTime = null;          return;
            case DATE:  this.uDate = null;          return;
            case DIC:   this.dVatID = null;         return;
            case MODE:  this.uSimpleMode = null;    return;
            case SUM:
                this.uAmount = null;
                this.mAmount = 0.0f;
        }
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Comparable<T> override

    /**
     * Compare bills according to dates (bill creation date | bill registration date)
     * @param o other Bill object
     * @return  0 => THIS == PARAM
     *          - => PARAM is "later" than THIS (compare result to "<0")
     *          + => THIS is "later" than PARAM (compare result to ">0")
     */
    @Override
    public int compareTo(@NonNull Bill o) {

        if (sortByRegTimeStamp == null) sortByRegTimeStamp = true;
        if (sortByRegTimeStamp) {
            return mRegistrationTimeStamp.compareTo(o.getRegTimeStamp());
        }
        else {
            if (getGivenBillDate() == null) {
                Crashlytics.log("Bill.compareTo :: getGivenBillDate is null");
                return 0;
            }
            return getGivenBillDate().compareTo(o.getGivenBillDate());
        }

    }
}
