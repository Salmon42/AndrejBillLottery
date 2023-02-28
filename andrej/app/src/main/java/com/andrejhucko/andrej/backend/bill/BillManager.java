package com.andrejhucko.andrej.backend.bill;

import java.util.*;
import java.util.regex.*;
import android.util.Log;
import android.support.v4.util.Pair;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionText.TextBlock;

public final class BillManager {

    private static final String TAG = "BillManager";

    /** Bill entry object holding plain data for registration                                     */
    private Bill bill = new Bill();
    /** Is scanning of DIČ (VatID) permitted?                                                     */
    private boolean extendedScanning;
    /**
     * Boolean array for keeping information of which entries are already stored
     * Indices for evaluation: (count = 8)
     * [ FIK | TIME | DATE | MODE | SUM | WRD | DIC | BKP ]
     * [  0  |  1   |  2   |  3   |  4  |  5  |  6  |  7  ]                                       */
    private boolean[] scanned = new boolean[8];

    /** Base set of patterns, iterated through [[INSERT METHOD HERE]]                             */
    private static final Map<Entry, Pattern> basePatterns;
    static {
        basePatterns = new HashMap<>();

        // ex-fik : Pattern.compile("(.*FIK.*|[0-9a-f]{8}[\\W\\w\\s][0-9a-f]{4}[\\W\\w\\s][0-9a-f]{4})")
        basePatterns.put(Entry.FIK,     Pattern.compile("(fik|fisk[aáä]ln[ií]|[0-9a-f]{8}|[0-9a-f]{4}|4[0-9a-f]{3})", Pattern.CASE_INSENSITIVE));
        basePatterns.put(Entry.TIME,    Pattern.compile(".*\\d{2}:\\d{2}(:\\d{2}){0,1}.*"));
        basePatterns.put(Entry.DATE,    Pattern.compile(".*\\d{1,2}[.,/-]\\d{1,2}[.,/-](\\d{2}|\\d{4}).*"));
        basePatterns.put(Entry.SUM,     Pattern.compile("\\s*\\d+[,.]\\d{1,2}\\s*(k.*){0,1}\\s*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
        basePatterns.put(Entry.MODE,    Pattern.compile("(re.+im|b[eěé].n.|zjednodu.*)", Pattern.CASE_INSENSITIVE));
        basePatterns.put(Entry.BKP,     Pattern.compile(".*(BKP|\\s+[0-9A-Z]{8}[-_.,][0-9A-Z]{8}[-_.,]).*")); // ([0-9A-Z]{8}\s*-){4}[0-9A-Z]{8}
        basePatterns.put(Entry.DIC,     Pattern.compile(".*DI[CČĆ].*", Pattern.CASE_INSENSITIVE));
        // Helper pattern for better total price detection
        basePatterns.put(Entry.WORD,    Pattern.compile("(celk.*|k[ ]+([uú]hrad.*|platb.*)|[cćč][aá]stka|hotov.*|sou.et)", Pattern.CASE_INSENSITIVE));
    }

    /** Extended set of patterns, used in custom evaluations in [[INSERT METHOD HERE]]            */
    public static final Map<String, Pattern> extendedPatterns;
    static {
        extendedPatterns = new HashMap<>();

        extendedPatterns.put("FIK", Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}", Pattern.CASE_INSENSITIVE));
        extendedPatterns.put("BKP", Pattern.compile("[0-9A-Z]{8}-[0-9A-Z]{8}", Pattern.CASE_INSENSITIVE));
        extendedPatterns.put("BKP_CLEAN", Pattern.compile("[0-9A-F]{8}-[0-9A-F]{8}", Pattern.CASE_INSENSITIVE));

        extendedPatterns.put("TIME", Pattern.compile("\\d{2}:\\d{2}(:\\d{2}){0,1}"));
        extendedPatterns.put("TIME_HRS", Pattern.compile("^\\d{2}"));
        extendedPatterns.put("TIME_MIN", Pattern.compile("^:\\d{2}"));

        extendedPatterns.put("SUM_INTPART", Pattern.compile("^\\d+"));  // Regex for detecting integer part of the price
        extendedPatterns.put("SUM_DECPART", Pattern.compile("\\d{1,2}")); // Regex for detecting the decimal part

        extendedPatterns.put("MODE_STD", Pattern.compile("b[eěé].n.", Pattern.CASE_INSENSITIVE));
        extendedPatterns.put("MODE_EZ", Pattern.compile("zjednodu.*", Pattern.CASE_INSENSITIVE));

        extendedPatterns.put("DIC", Pattern.compile("CZ[0-9]{8,10}", Pattern.CASE_INSENSITIVE));

        // datePatterns
        extendedPatterns.put("DATE_BASE", Pattern.compile("\\d{1,2}[./-]\\d{1,2}[./-]\\d{4}")); // Basic date : 10.10.2010
        extendedPatterns.put("DATE_SHORT", Pattern.compile("\\d{2}[./-]\\d{2}[./-]\\d{2}"));    // Short date : 10.10.10

        extendedPatterns.put("DATE_YEAR", Pattern.compile("\\d{4}"));
        extendedPatterns.put("DATE_MONTH", Pattern.compile("(?<!^)\\d{2}\\D+"));
        extendedPatterns.put("DATE_DAY", Pattern.compile("^\\d{2}"));

        extendedPatterns.put("DATE_YEAR_SHORT", Pattern.compile("\\d{2}$"));
        extendedPatterns.put("DATE_MONTH_SHORT", Pattern.compile("(?<!^)\\D\\d\\D+"));
        extendedPatterns.put("DATE_DAY_SHORT", Pattern.compile("^\\d"));
    }

    /* ****************************************************************************************** */

    /** Helper class for parsing data for clean code                                              */
    private Parser parser = new Parser(basePatterns, extendedPatterns); //, bill
    /** Helper coordinations object for better evaluation of found "sum-word"                     */
    private List<Coords> foundSumWordLocations = null;
    /** Set of found prices, bundled with their coordinates                                       */
    private Map<Coords, Float> foundPrices = null;

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Methods

    /**
     * Nice constructor.
     * @param extendedScanning scan DIC or not? (vatID)
     */
    public BillManager(boolean extendedScanning) {
        this.extendedScanning = extendedScanning;
    }

    /**
     * called in {@link com.andrejhucko.andrej.backend.ocr.TextRecognitionProcessor.Listener#parseDetections(List)}
     * @param detections list of text block objects containing detected characters
     * @return another list of blocks that contain useful information
     */
    public Pair<HashSet<Entry>, ArrayList<TextBlock>> parseDetections(List<TextBlock> detections) {

        int listSize = 0;
        Pair<HashSet<Entry>, ArrayList<TextBlock>> detectedBlocks = new Pair<>(
            new HashSet<Entry>(),
            new ArrayList<TextBlock>()
        );
        if (detectedBlocks.first == null || detectedBlocks.second == null)
            return null;

        // for each bulk of detections reset all found prices
        if (!scanned[Entry.SUM.index()]) {
            foundSumWordLocations = new ArrayList<>();
            foundPrices = new HashMap<>();
        }

        for (int i = 0; i < detections.size(); i++) {
            TextBlock block = detections.get(i);
            // block.getText() <==> block.getValue()
            if (block != null && !block.getText().equals("")) {

                Log.wtf(TAG + " :: BLOCK ::: ", block.getText());

                // List of successfull pattern matches for each textblock
                List<Entry> itemList = new ArrayList<>();

                // Iterate through all base patterns
                for (Map.Entry<Entry, Pattern> set : basePatterns.entrySet()) {

                    // Skip unnecessary regex matching
                    if (scanned[set.getKey().index()]) continue;

                    // No extended scanning? skip DIC
                    if (set.getKey() == Entry.DIC && !extendedScanning) continue;

                    // Evaluate a block that has probability of containing the right data
                    Matcher matcher = set.getValue().matcher(block.getText());
                    if (matcher.find()) {
                        itemList.add(set.getKey());
                    }
                }// end for

                for (Entry item : itemList) {
                    // call the Extract method here, the method stores any valid data in storage
                    boolean hasExtracted = extract(block, item);
                    if (hasExtracted) {
                        listSize++;
                        detectedBlocks.first.add(item);
                        detectedBlocks.second.add(block);
                        Log.wtf(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~ FOUND: "
                                + item + " ::: " + block.getText());
                    }
                }
            }
        }// end for

        boolean ret = evalFoundPrices();
        if (!ret) {
            detectedBlocks.first.remove(Entry.SUM);
        }
        detectedBlocks.first.remove(Entry.WORD);

        return listSize > 0 ? detectedBlocks : null;
    }

    /**
     * Just code shortening inside {@link BillManager#parseDetections(List)}
     * In the textboxes there have been found many prices (not just in one block)
     * so any evaluation of price should be done after parsing all boxes
     */
    private boolean evalFoundPrices() {

        boolean shouldSetNewPrice = false;

        if (!scanned[Entry.SUM.index()]) {

            // check & evaluate all prices found - if price already scanned, the objects are null
            if (foundSumWordLocations.size() > 0 && foundPrices.size() > 0) {
                // TODO: issue where the price is purely UNDER the sumword
                Log.wtf(TAG, "Detection iterations ended, having " + foundPrices.size() + " price candidates");

                // some evaluation variables
                float minVerticalOffset = Float.POSITIVE_INFINITY;
                float curPrice = 0.0f;
                float curVerticalOffset;
                Coords curWord = null;
                Coords candidatePrice = null;

                for (Coords sumWordLoc : foundSumWordLocations) {
                    for (Map.Entry<Coords, Float> entry : foundPrices.entrySet()) {
                        Log.wtf(TAG, "foundPrices Map iteration:::" + entry.getValue());

                        curVerticalOffset = Coords.getVOffset(sumWordLoc, entry.getKey());
                        if (entry.getValue() > curPrice) {
                            if (minVerticalOffset > curVerticalOffset) {
                                if (curVerticalOffset < sumWordLoc.getHeight()) {
                                    Log.wtf(TAG, "Price: " + entry.getValue() + " WAS flagged as new shortest distance!");
                                    minVerticalOffset = curVerticalOffset;
                                    curPrice = entry.getValue();
                                    curWord = sumWordLoc;
                                    candidatePrice = entry.getKey();
                                }
                            }
                        }
                    }
                }

                if (candidatePrice != null) {

                    Log.wtf(TAG, "AFTER EVALUATION:");
                    Log.wtf(TAG, "FOUND WORD: " + curWord);
                    Log.wtf(TAG, "NEAREST PRICE: " + candidatePrice.toString());

                    if (curPrice > 0.001f) {
                        // Check whether the bill object has already stored the value
                        // (was the value recognized and removed by user?)
                        if (bill.getFloatAmount() > 0.001f) { // does it contain previous discarded value?
                            if ( !(Math.abs(bill.getFloatAmount() - curPrice) < 0.001f)) {
                                shouldSetNewPrice = true; // Matched new value
                            }
                            // else: the same value, do not set anything
                        }
                        else {
                            shouldSetNewPrice = true;
                        }
                    }

                    if (shouldSetNewPrice) {
                        scanned[5] = true; // "SUM_WRD"
                        scanned[4] = true; // "SUM"
                        bill.setEntry(Entry.SUM, Math.round(curPrice*100));
                        Log.wtf(TAG, "RECOGNIZED PRICE <<<<<<<<<<<<<<<<<<<<" + curPrice + ">>>>>>>>>>>>>>>>>>>>>>>>>");
                    }

                }

            }// end of price finding

        }

        // Reset these every incoming frame (Frame = array of blocks)
        foundSumWordLocations = null;
        foundPrices = null;
        return shouldSetNewPrice;

    }

    /**
     * Extraction method from single textbox.
     * @param block contains the characters & words & values needed for registration
     * @param entry type of bill entry that should be looked for
     * @return true if the entry was successfully extracted
     */
    @SuppressWarnings("unchecked")
    private boolean extract(TextBlock block, Entry entry) {

        String exactValue = "";
        for (FirebaseVisionText.Line line : block.getLines()) {
            exactValue = exactValue.concat(line.getText());
        }

        Log.wtf(TAG + " :: BLOCKDETECTION ::: " + entry + " ::: ", exactValue);

        Object result = parser.get(block, entry);

        if (result != null && !bill.isInErrList(entry, result)) {
            switch (entry) {

                case FIK:
                    bill.setEntry(Entry.FIK, result);
                    scanned[0] = true;
                    break;

                case BKP:
                    bill.setEntry(Entry.BKP, result);
                    scanned[7] = true;
                    break;

                case DIC:
                    bill.setEntry(Entry.DIC, result);
                    scanned[6] = true;
                    break;

                case MODE:
                    bill.setEntry(Entry.MODE, result);
                    scanned[3] = true;
                    break;

                case DATE:
                    bill.setEntry(Entry.DATE, result);
                    scanned[2] = true;
                    break;

                case TIME:
                    bill.setEntry(Entry.TIME, result);
                    scanned[1] = true;
                    break;

                case SUM:
                    foundPrices.putAll((Map<Coords, Float>) result);
                    break;

                case WORD:
                    foundSumWordLocations.addAll((List<Coords>) result);
                    break;
            }
            return true;
        }
        return false;
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///  Methods for outside usage

    /**
     * Get raw entry (as the object to be inserted to JSON for registration)
     * @param entry only fik,bkp,date,time,sum,mode
     * @return the object (string, integer or boolean, you should cast it yourself)
     */
    public Object getEntry(Entry entry) {
        switch (entry) {
            case FIK:   return bill.getFIK();
            case BKP:   return bill.getBKP();
            case DATE:  return bill.getDate();
            case TIME:  return bill.getTime();
            case SUM:   return bill.getAmount();
            case MODE:  return bill.getSimpleMode();
            default: return null;
        }
    }

    public void setManualEntry(Entry entry, Object data) {
        bill.setEntry(entry, data);
        scanned[entry.index()] = true;
        if (entry == Entry.SUM) {
            scanned[Entry.WORD.index()] = true;
        }
    }

    /**
     * JUST MARKS AS UNSCANNED in the register, does not actually remove the string/integer
     * @param entry type of bill entry currently stored in billstorage
     */
    public void removeEntry(Entry entry) {
        bill.removeEntry(entry);
        scanned[entry.index()] = false;
        if (entry == Entry.SUM) {
            scanned[Entry.WORD.index()] = false;
        }
        bill.addToErrList(entry);
    }

    /**
     * Show string entries to be shown in textviews
     * @param entry only fik,bkp,date,time,sum,mode,dic
     * @return string-readable representation of entry
     */
    public String getDispEntry(Entry entry) {
        switch (entry) {
            case FIK:   return bill.getFIK();
            case BKP:   return bill.dispBKP();
            case DATE:  return bill.dispDate();
            case TIME:  return bill.getTime();
            case SUM:   return bill.dispAmount();
            case MODE:  return bill.dispSimpleMode();
            case DIC:   return bill.getVatID();
            default: return "";
        }
    }

    /**
     * Quite self-documenting.
     * @param entry one of {@link Entry} values
     * @return bool value if scanned or not
     */
    public boolean isScanned(Entry entry) {
        return scanned[entry.index()];
    }

    /**
     * Evaluate first 5 flags inside {@link BillManager#scanned}
     * @return false if any of the scanned bits is false
     */
    public boolean isComplete() {

        for (int i = 1; i < 6; i++) {
            if (!scanned[i]) return false;
        }
        // extended scanning checks for stored DIC
        if (extendedScanning && !scanned[6]) return false;
        // FIK or BKP must be present
        return (scanned[0] || scanned[7]);

    }

    /**
     * Storing unregistered bill data to shared preferences
     * @return JSON object in String representation
     */
    public String getStorePayload() {
        return bill.getBillStorageString();
    }

    /**
     * Preparing JSON from bill data in order to be sent to uctenkovka
     * @return JSON object in String representation
     */
    public String getRegPayload() {
        return bill.getRegPayload();
    }
}
