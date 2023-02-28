package com.andrejhucko.andrej.backend.utility;

import org.json.*;
import java.util.*;
import android.net.*;
import android.text.*;
import android.util.*;
import android.os.Build;
import android.content.*;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import com.crashlytics.android.Crashlytics;
import com.andrejhucko.andrej.*;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.network.*;
import com.andrejhucko.andrej.frontend.dialogs.filter.BillFilterChoice;

public final class App {

    private static final String TAG = "App";

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Constants
    public static final int FRAGMENT_COUNT = 9;
    public static final String RUN_SCAN = "RUN_APP_WITH_SCAN";

    static final String ACCESS_TOKEN = "accessToken";
    static final String REFRESH_TOKEN = "refreshToken";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Scan Activity result codes
    // ASR - Activity Scan Result
    /** Activity ends successfully.                                                               */
    public static final int ASR_SUCCESS = 0;
    /** Activity ends with finished scanning (stored in prefs), because user has no account       */
    public static final int ASR_NO_ACCOUNT = 1;
    /** Bill was sent - but response was null -> no internet connection/this kind of error        */
    public static final int ASR_NETWORK_ERR = 2;
    /** Bill is already registered, marked as duplicate                                           */
    public static final int ASR_DUPLICATE = 3;
    /** Bill is outside the 1month (+- some days, idk) window, unable to register                 */
    public static final int ASR_OUTDATED = 4;
    /** Activity fell into exception, unusual occurence                                           */
    public static final int ASR_UNEXPECTED = 5;
    /** ... */
    public static final int ASR_DO_NOT_RESPOND = 6;
    /** User has invalid tokens - both access & refresh token, he must relog                      */
    public static final int ASR_RELOG = 7;

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Date-oriented methods

    /**
     * Reformat YYYY-MM-DD to DD. MM. YYYY
     * @param date string with previous format
     * @return string with new formatting
     */
    public static String printDate(String date) {
        if (date == null) return "";
        return date.substring(8, 10) + ". " + date.substring(5, 7) + ". " + date.substring(0, 4);
    }

    /**
     * Create date in format DD. MM. YYYY
     * @param cal calendar with data to be extracted
     * @return requested printable date
     */
    public static String printDate(Calendar cal) {
        return cal.get(Calendar.DATE) + ". " + (cal.get(Calendar.MONTH)+1) + ". " + cal.get(Calendar.YEAR);
    }

    /**
     * Convert string representation of date to {@link Date} object
     * Accepted formats:
     *      YYYY-MM-DDTHH:MM:SS~    format of Uctenkovka bill JSON time stamp
     *      YYYY-MM-DD              classic format
     * @param date the string representation
     * @return date object
     */
    public static Date parseDate(String date) {
        if (date == null) return null;
        if (date.length() < 10) return null;

        Calendar cal = Calendar.getInstance();

        int year = Integer.valueOf(date.substring(0, 4));
        int month = Integer.valueOf(date.substring(5, 7));
        int day = Integer.valueOf(date.substring(8, 10));

        if (date.length() >= 19) {
            int hour = Integer.valueOf(date.substring(11, 13));
            int min = Integer.valueOf(date.substring(14, 16));
            int sec = Integer.valueOf(date.substring(17, 19));

            cal.set(year, month, day, hour, min, sec);
        }
        else {
            cal.set(year, month, day, 0, 0, 0);
        }
        return new Date(cal.getTimeInMillis());
    }

    /**
     * Basically Now().toString() ... in certain formatting (as uctenkovka format for TS)
     * @return string timestamp of now()
     */
    public static String currentTimestamp() {

        Calendar cal = Calendar.getInstance();

        String month = String.valueOf((cal.get(Calendar.MONTH)+1 < 10) ? "0"+(cal.get(Calendar.MONTH)+1) : (cal.get(Calendar.MONTH)+1));
        String day = String.valueOf((cal.get(Calendar.DAY_OF_MONTH) < 10) ? "0"+cal.get(Calendar.DAY_OF_MONTH) : cal.get(Calendar.DAY_OF_MONTH));

        String hour = String.valueOf((cal.get(Calendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(Calendar.HOUR_OF_DAY) : cal.get(Calendar.HOUR_OF_DAY));
        String min = String.valueOf((cal.get(Calendar.MINUTE) < 10) ? "0"+cal.get(Calendar.MINUTE) : cal.get(Calendar.MINUTE));
        String sec = String.valueOf((cal.get(Calendar.SECOND) < 10) ? "0"+cal.get(Calendar.SECOND) : cal.get(Calendar.SECOND));

        return  cal.get(Calendar.YEAR) + "-" + month + "-" + day + "T" + hour + ":" + min + ":" + sec;

    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Shared preferences methods

    /** Load bills for LotteryFrag (the calendar view). Please just don't load all of them. */
    public static @NonNull List<Bill> fetchAllBills(@NonNull Context context) {

        Set<String> draws = new HashSet<>();
        for (String user : Storage.def(context).getAccList()) {
            draws.addAll(Storage.def(context).getDraws(user));
        }

        int size = 0;
        for (String draw : draws) {
            size += Storage.rb(context, draw).getAll().size();
        }

        if (size == 0) return new ArrayList<>();
        Bill[] array = new Bill[size];
        boolean filter = Storage.def(context).getBillSortStyle();
        int c = 0;

        for (String draw : draws) {
            SharedPreferences pref = Storage.rb(context, draw);

            for (String key : pref.getAll().keySet()) {
                Bill bill = new Bill(pref.getString(key, null));
                bill.setComparationFilter(filter);
                array[c] = bill;
                c++;
            }

        }
        return Arrays.asList(array);

    }

    /**
     * Fetch bills from shared preferences storage, fetches from all accounts.
     * Searches for storage of bills determined by current filter choice
     *  - Unsent bills
     *  - Actual draw date
     *  - some older draw date
     * @param context for getting the shared preferences
     * @return empty or full arraylist
     */
    public static @NonNull List<Bill> fetchBills(@NonNull Context context) {

        String draw = Storage.def(context).getBillFilter().getDraw();
        SharedPreferences pref;

        switch (draw) {
            case DefStorage.BILL_FILTER.D_ACTUAL: pref = Storage.rb(context, DrawDate.getNextLotteryDate()); break;
            case DefStorage.BILL_FILTER.D_UNSENT: pref = Storage.ub(context); break;
            default: pref = Storage.rb(context, draw); break;
        }

        int size = pref.getAll().size();
        if (size == 0) return new ArrayList<>();
        boolean filter = Storage.def(context).getBillSortStyle();
        Bill[] array = new Bill[size];
        int c = 0;

        for (String key : pref.getAll().keySet()) {
            Bill bill = new Bill(pref.getString(key, null));
            bill.setComparationFilter(filter);
            array[c] = bill;
            c++;
        }

        List<Bill> list = Arrays.asList(array);
        Collections.sort(list, Collections.<Bill>reverseOrder());
        return list;

    }

    /**
     * Iterate through preference stringset (default pref) and get player id
     * method is used for mapping playerId -> accountname (phone|mail)
     * just to show on bill detail who registered that bill (playerId is stored in bill json)
     *
     * @param context  for prefs
     * @param playerID integer from bill json
     * @return string username
     */
    public static String findPlayerById(Context context, Integer playerID) {
        for (String acc : Storage.def(context).getAccList()) {
            if (Storage.def(context).getPid(acc) == playerID) {
                return acc;
            }
        }
        return null;
    }

    /**
     * Store in preferences "bills_to_send" (see in App constants) the scanned bill entries
     * store to the prefs by key:value => key = FIK|BKP (whichever is not null)
     */
    public static void storeBill(Context context, BillManager manager) {

        String key = (String) (manager.isScanned(Entry.FIK)
                ? manager.getEntry(Entry.FIK)
                : manager.getEntry(Entry.BKP));
        Storage.ub(context).edit().putString(key, manager.getStorePayload()).apply();

    }

    /** Find and remove single bill data node */
    public static void removeBill(Context context, BillManager manager) {

        String needle = (String) ((manager.isScanned(Entry.FIK))
                ? manager.getEntry(Entry.FIK)
                : manager.getEntry(Entry.BKP));
        Storage.remove(Storage.ub(context), needle);

    }

    /** Find and remove single bill data node */
    public static void removeBill(Context context, Bill bill) {

        String needle = null;
        if (bill.getFIK() != null) needle = bill.getFIK();
        else if (bill.getBKP() != null) needle = bill.getBKP();
        Storage.remove(Storage.ub(context), needle);

    }

    /** Recount TBC for given user. */
    public static void recountTransientBills(Context context, String user) {
        if (context == null || user == null) return;

        Set<String> draws = Storage.def(context).getDraws(user);
        int tbc = 0;

        for (String draw : draws) {
            Map<String, ?> bills = Storage.rb(context, draw).getAll();
            for (String key : bills.keySet()) {
                try {
                    JSONObject bill = new JSONObject((String) bills.get(key));
                    Status status = Status.get(bill.getString(JsonKey.STATUS.n()));

                    if (status == Status.NEW || status == Status.VERIFIED || status == Status.IN_DRAW) {
                        tbc++;
                    }
                }
                catch (Exception e) {
                    Log.wtf(TAG, "JSONException");
                    Log.wtf(TAG, e.getMessage());
                    return;
                }
            }
        }
        Storage.def(context).setTbc(user, tbc);

    }

    /**
     * Store the auth tokens to shared preferences.
     * @param context for SharedPreferences
     * @param user    the username, which is the key to the name->value storage in SP
     * @param tokens  auth tokens
     */
    public static void storeAuthTokens(Context context, String user, AuthTokens tokens) {

        Storage.def(context).setAccToken(user, tokens.access());

        CryptoHandler handler = new CryptoHandler(context);
        String encryptedToken = handler.encrypt(tokens.refresh());
        String hashedUser = CryptoHandler.md5(user);

        Storage.auth(context).edit().putString(hashedUser, encryptedToken).apply();

    }

    /**
     * Retrieves authorisation tokens for given user
     * Stores crashlytics if exception thrown
     * @param context given context
     * @param user    the user
     * @return given Container with access and refresh tokens
     */
    public static AuthTokens getAuthTokens(Context context, String user) {

        String accessToken = Storage.def(context).getAccToken(user);
        String refreshToken = Storage.auth(context).getString(CryptoHandler.md5(user), null);

        if (accessToken == null || refreshToken == null) {
            return null; // only if the user has never added account...
        }

        CryptoHandler handler = new CryptoHandler(context);
        refreshToken = handler.decrypt(refreshToken);

        return new AuthTokens(accessToken, refreshToken);
    }

    /**
     * Removes current account.
     * @param context required for sharedpreferences
     */
    public static void purgeAccountData(Context context) {
        final DefStorage store = Storage.def(context);
        final String user = store.getCurrentAcc();
        final Set<String> draws = store.getDraws(user);
        final int pid = store.getPid(user);

        for (String prefname : draws) {
            SharedPreferences pref = Storage.rb(context, prefname);

            SharedPreferences.Editor editor = pref.edit();
            for (String key : pref.getAll().keySet()) {
                try {
                    JSONObject obj = new JSONObject(pref.getString(key, null));
                    if (pid == obj.getInt(JsonKey.PLAYER.n())) {
                        editor.remove(key);
                    }
                } catch (JSONException e) {
                    Crashlytics.logException(e);
                }
            }
            editor.apply();

        }

        // remove all from basic storage
        store.removeAccount(user);
        // remove refresh token
        Storage.auth(context).edit().remove(CryptoHandler.md5(user)).apply();

        if (!store.hasAnyAccount()) {
            store.setBillFilter(
                new BillFilterChoice(
                    DefStorage.BILL_FILTER.D_UNSENT,
                    DefStorage.BILL_FILTER.P_ALL
                )
            );
        }

    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Other code shortening methods


    /*

    public static <T> T n(T obj) {
        return Objects.requireNonNull(obj);
    }
    */

    /** Wrapper for Objects.requireNonNull() */
    public static <T> T n(T obj, String message) {
        return Objects.requireNonNull(obj, message);
    }


    /**
     * With help of connectivitymanager, get info about whether the phone is connected to network.
     * @param context for {@link ConnectivityManager}
     * @return true when the phone has present network connection.
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Helper function for calculating PX from DP.
     * @param context context
     * @param dp      density-independent pixels
     * @return        Number of pixels
     */
    public static int dpToPixel(Context context, int dp) {
        Resources res = context.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp , res.getDisplayMetrics()));
    }

    /**
     * With current knowledge, all mappings cannot be done - new ones are reported
     * @param stat string status, see {@link JsonKey}
     * @return R.string
     */
    public static int mapWinStat(String stat) {

        switch (stat) {
            case "BACC_SUCCESS": return R.string.bill_detail_winning_state_bacc_succ;
            default:
                Reporter.send(false,"App.mapWinStat: new encountered value", stat);
                return R.string.epsilon;
        }

    }

    /** Generate the HTML-like textview content */
    public static Spanned genPolicyText() {

        final String POLICY = "<p>Tento dokument nabývá účinnosti od 19.12.2018.</p>\n" +
            "      <br>\n" +
            "      <p>Andrej Hučko (\"vývojář\") je autorem aplikace Andrej pro účtenkovou loterii (\"aplikace\"). Tato stránka je určena k informování uživatelů aplikace o ochraně, využívání a způsobech zpracování osobních dat (\"dokument\"). Používaním této aplikace souhlasíte se zpracováváním údajů.</p>\n" +
            "      \n" +
            "      <br>\n" +
            "      <h2>Informace o zpracování údajů</h2>\n" +
            "\n" +
            "      <p>Tento software je tzv. aplikace třetí strany. Aplikace zpracovává jenom ty údaje, které jsou nevyhnutelně potřebné pro správné zprostředkování služby. Tyto údaje jsou udělovány Vámi při přístupu do Vašeho hráčského účtu, jmenovitě <b><u>e-mail</u></b> a <b><u>telefonní číslo</u></b>. Údaje jsou uloženy ve Vašem zařízení s aplikací a nejsou nikam odesílány.</p>\n" +
            "      <p>Aplikace využívá služby třetích stran, které mohou sbírat některé informace pro identifikaci Vašeho hráčského účtu (na Účtenkovce), nebo typu Vašeho zařízení. Jedná se o služby <b>Účtenkovka</b>, <b>Google Play Services</b>, <b>Firebase Analytics</b> a <b>Crashlytics</b>. Odkazy na informace o ochraně osobních údajů jednotlivých služeb jsou uvedeny zde:</p>\n" +
            "\n" +
            "      <ul>\n" +
            "        <li>\n" +
            "          Účtenkovka <a id=\"other_link\" href=\"https://www.uctenkovka.cz/docs/Uctenkovka-ochrana-osobnich-udaju.pdf\">(www.uctenkovka.cz/docs/Uctenkovka-ochrana-osobnich-udaju.pdf)</a>\n" +
            "        </li>\n" +
            "\n" +
            "        <li>\n" +
            "          Google Play Services <a id=\"other_link\" href=\"https://policies.google.com/privacy\">(policies.google.com/privacy)</a>\n" +
            "        </li>\n" +
            "\n" +
            "        <li>\n" +
            "          Firebase Analytics <a id=\"other_link\" href=\"https://firebase.google.com/policies/analytics\">(firebase.google.com/policies/analytics)</a>\n" +
            "        </li>\n" +
            "\n" +
            "        <li>\n" +
            "          Crashlytics <a id=\"other_link\" href=\"http://try.crashlytics.com/terms/privacy-policy.pdf\">(try.crashlytics.com/terms/privacy-policy.pdf)</a>\n" +
            "        </li>\n" +
            "      </ul>\n" +
            "\n" +
            "      <br>\n" +
            "      <h2>Bezpečnost</h2>\n" +
            "      <p>Veškeré údaje jsou zpracovávány a chráněny bezpečnostními mechanismy zajišťujícími jejich maximální možnou ochranu před neoprávněným přístupem nebo přenosem, před jejich ztrátou nebo zničením, jakož i před jiným možným zneužitím. Citlivé údaje potřebné pro zprostředkování služby jsou kryptograficky zabezpečeny a veškerá síťová komunikace využívá šifrované spojení.</p>\n" +
            "      \n" +
            "      <br>\n" +
            "      <h2>Odkazy na jiné stránky</h2>\n" +
            "      <p>Tato aplikace může obsahovat odkazy na jiné stránky. Pokud otevřete takový odkaz, budete přesměrováni na danou stránku. Tyto stránky nejsou spravovány vývojářem, a proto nenese žádnou odpovědnost za jejich obsah a služby.</p>\n" +
            "\n" +
            "      <br>\n" +
            "      <h2>Změny dokumentu</h2>\n" +
            "      <p>Vývojář si vyhrazuje právo změnit obsah tohoto dokumentu. Každá změna bude ohlášena v aktualizaci aplikace na portálu Google Play. Změny nabývají účinnosti v den zveřejnění aktualizace.</p>\n" +
            "\n" +
            "      <br>\n" +
            "      <h2>Kontakt</h2>\n" +
            "      <p>Jestli máte nějaké otázky ohledně dokumentu, můžete kontaktovat vývojáře na e-mail <a href=\"mailto:info@andrej.zaridi.to\">info@andrej.zaridi.to</a>.</p>\n";


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(POLICY, Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(POLICY);
        }

    }

}

