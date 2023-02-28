package com.andrejhucko.andrej.backend.utility;

import java.util.*;
import android.content.*;
import android.preference.PreferenceManager;
import com.andrejhucko.andrej.frontend.dialogs.filter.BillFilterChoice;

public class DefStorage {

    /** Could be enum, but whatever - used as key & value for shared preferences */
    public static class BILL_FILTER {
        public static final String DRAW = "bill_filter_draw";
        public static final String D_UNSENT = "fbd_unsent";
        public static final String D_ACTUAL = "fbd_actual";

        public static final String PICK = "bill_filter_pick";
        public static final String P_ALL    = "fbp_all";
        public static final String P_INDRAW = "fbp_indraw";
        public static final String P_WINNING = "fbp_winning";
        public static final String P_NONWIN = "fbp_nonwin";
        public static final String P_WRONG  = "fbp_wrong";
    }

    /** Keys for default shared preferences */
    public static class SETTINGS {
        /// Boolean preferences (SETTINGS Fragment) - stored in default preferences!
        public static final String FLOATBTN = "prefs_show_floating_button";
        public static final String STARTCAM = "prefs_scanning_at_start";
        public static final String HINTS = "prefs_show_help";
        public static final String EXTSCAN = "prefs_permit_extended_scanning";
    }

    /// Default shared preferences contain this as well: ID = email or phone
    private SharedPreferences storage;

    DefStorage(Context context) {
        storage = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Account-related

    public void addAccount(String username, String type) {
        addToAccList(username);
        setAccType(username, type);
        setCurrentAcc(username);
    }

    void removeAccount(String username) {
        removeFromAccList(username);
        storage.edit()
            .remove("current_account")
            .remove(ACCOUNT_TYPE + username)
            .remove(username)
            .remove(TBC + username)
            .remove(BLF + username)
            .remove(VLD + username)
            .remove(PID + username)
            .remove(RB + username)
            .apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    /** STRING: email of phone of current used account */
    public static final String CURRENT_ACC = "current_account";

    public String getCurrentAcc() {
        return storage.getString(CURRENT_ACC, null);
    }

    public String getCurrentAccPrintable(String defValue) {
        return storage.getString(CURRENT_ACC, defValue);
    }

    public void setCurrentAcc(String user) {
        storage.edit().putString(CURRENT_ACC, user).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Account List

    /** STRINGSET: (e-mail OR phone) for the user added to whenever user adds & server validates */
    public static final String ACC_LIST = "account_list";

    public Set<String> getAccList() {
        return storage.getStringSet(ACC_LIST, new HashSet<String>());
    }

    private void setAccList(Set<String> accList) {
        storage.edit().putStringSet(ACC_LIST, accList).apply();
    }

    private void addToAccList(String user) {
        Set<String> list = getAccList();
        boolean added = list.add(user);
        if (added) {
            setAccList(list);
            storage.edit().putInt(ACCOUNT_COUNTER, storage.getInt(ACCOUNT_COUNTER, 0) + 1).apply();
        }
    }

    private void removeFromAccList(String user) {
        Set<String> list = getAccList();
        boolean removed = list.remove(user);
        if (removed)  {
            setAccList(list);
            storage.edit().putInt(ACCOUNT_COUNTER, storage.getInt(ACCOUNT_COUNTER, 0) - 1).apply();
        }
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Account Counter

    /** BOOLEAN: Did user already add an account to the app? */
    private static final String ACCOUNT_COUNTER = "account_counter";

    /** Updated AUTOMATICALLY with addToAccList & removeFromAccList */
    public boolean hasAnyAccount() {
        return storage.getInt(ACCOUNT_COUNTER, 0) > 0;
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Account Token

    /** key: ID ; value: the access token */
    public String getAccToken(String user) {
        return storage.getString(user, null);
    }

    public void setAccToken(String user, String token) {
        storage.edit().putString(user, token).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Account Type

    /** key: "#ACT#" + ID ; value: the type of account (phone or mail) */
    private static final String ACCOUNT_TYPE = "#ACT#";

    public String getAccType(String user) {
        // "E-mail" | "Telefon"
        return storage.getString(ACCOUNT_TYPE + user, "E-mail");
    }

    private void setAccType(String user, String type) {
        storage.edit().putString(ACCOUNT_TYPE + user, type).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Transient Bill count

    /** key: "#TBC#"+ID ; value : int : transient bill counter
        stores count of bills with these attributes: NEW, VERIFIED, IN_DRAW */
    private static final String TBC = "#TBC#";

    public int getTbc(String user) {
        return storage.getInt(TBC + user, 0);
    }

    public void setTbc(String user, int value) {
        storage.edit().putInt(TBC + user, value).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Player ID

    /** key: "#PID#"+ID ; value : int : the played ID assigned to the account credential */
    private static final String PID = "#PID#";

    public int getPid(String user) {
        return storage.getInt(PID + user, -1);
    }

    public void setPid(String user, int value) {
        storage.edit().putInt(PID + user, value).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Bill Last Fetch Timestamp

    /** key: "#BLF#"+ID ; value : str : the timestamp of most recent bill registration
        BLF = Bill Last Fetch */
    private static final String BLF = "#BLF#";

    public String getBlf(String user) {
        return storage.getString(BLF + user, null);
    }

    public void setBlf(String user, String blf) {
        storage.edit().putString(BLF + user, blf).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  User tokens validity

    /** key: "#VLD#"+ID ; value : bool :whether the account has valid tokens - yes=not stored */
    private static final String VLD = "#VLD#";

    public boolean isValidUser(String user) {
        return storage.getBoolean(VLD + user, true);
    }

    public void invalidateUser(String user) {
        storage.edit().putBoolean(VLD + user, false).apply();
    }

    public void setValidUser(String user) {
        storage.edit().remove(VLD + user).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Registered bills pointer set

    // key: "#RB#"+ID ; value : stringset : set of draws, where the user has downloaded his bills
    private static final String RB = "#RB#";

    public Set<String> getDraws(String user) {
        return storage.getStringSet(RB + user, new HashSet<String>());
    }

    public void setDraws(String user, Set<String> draws) {
        storage.edit().putStringSet(RB + user, draws).apply();
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Sorting... is it even used?

    public boolean getBillSortStyle() {
        // sort type for MyBillsFrag -> true / false
        final String BILL_SORTBY = "bill_sort_by";
        return storage.getBoolean(BILL_SORTBY, true);
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Bill Filter - the new one

    public BillFilterChoice getBillFilter() {
        BillFilterChoice choice = new BillFilterChoice();
        choice.setDraw(getBillFilter(BILL_FILTER.DRAW));
        choice.setPick(getBillFilter(BILL_FILTER.PICK));
        return choice;
    }

    public void setBillFilter(BillFilterChoice choice) {
        setBillFilter(BILL_FILTER.DRAW, choice.getDraw());
        setBillFilter(BILL_FILTER.PICK, choice.getPick());
    }

    private String getBillFilter(String type) {
        switch (type) {
            case BILL_FILTER.DRAW: return storage.getString(BILL_FILTER.DRAW, BILL_FILTER.D_ACTUAL);
            case BILL_FILTER.PICK: return storage.getString(BILL_FILTER.PICK, BILL_FILTER.P_ALL);
            default: throw new IllegalArgumentException();
        }
    }

    private void setBillFilter(String type, String value) {
        switch (type) {
            case BILL_FILTER.DRAW: storage.edit().putString(BILL_FILTER.DRAW, value).apply(); return;
            case BILL_FILTER.PICK: storage.edit().putString(BILL_FILTER.PICK, value).apply(); return;
            default: throw new IllegalArgumentException();
        }
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Settings

    public boolean getSetting(String setting, boolean defValue) {
        return storage.getBoolean(setting, defValue);
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  First run for policy pop

    public boolean isThisFirstRun() {
        return storage.getBoolean("first_run", true);
    }

    public void setFirstRun() {
        storage.edit().putBoolean("first_run", false).apply();
    }

}
