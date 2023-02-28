package com.andrejhucko.andrej.backend.bill;

import java.text.*;
import java.util.*;
import java.util.regex.*;
import android.util.Log;
import android.support.annotation.*;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

public class Parser {

    private static final String TAG = "Parser";

    private Map<Entry, Pattern> base;
    private Map<String, Pattern> ext;

    Parser(Map<Entry, Pattern> base, Map<String, Pattern> ext) {
        this.base = base;
        this.ext = ext;
    }

    public Object get(@NonNull FirebaseVisionText.TextBlock block, Entry entry) {

        switch (entry) {
            case FIK:   return getFIK(block);
            case BKP:   return getBKP(block);
            case DIC:   return getDIC(block);
            case MODE:  return getMode(block);
            case DATE:  return getDate(block);
            case TIME:  return getTime(block);
            case SUM:   return getSums(block);
            case WORD:  return getWords(block);
        }
        return null;

    }

    @Nullable
    private String getFIK(@NonNull FirebaseVisionText.TextBlock block) {
        
        String blockString = "";
        for (FirebaseVisionText.Line line : block.getLines()) {
            blockString = blockString.concat(line.getText());
        }

        List<String> results = new ArrayList<>();
        Matcher matcher = ext.get("FIK").matcher(blockString);
        while (matcher.find()) {
            results.add(matcher.group());
        }

        if (results.size() > 1) {
            Log.wtf(TAG, "getFIK: more than 1 result, excuse me what the f-");
        }

        return results.size() > 0 ? results.get(0) : null;
    }

    @Nullable
    private String getBKP(@NonNull FirebaseVisionText.TextBlock block) {

        String blockString = "";
        for (FirebaseVisionText.Line line : block.getLines()) {
            blockString = blockString.concat(line.getText());
        }

        blockString = blockString
                .replaceAll("_", "-")
                .replaceAll("\\.", "-")
                .replaceAll(",", "-");

        Matcher matcher = ext.get("BKP").matcher(blockString);
        if (matcher.find()) {
            Log.wtf(TAG, "BKP <<<<<<<<<<<<<<<<<<<<" + matcher.group() + ">>>>>>>>>>>>>>>>>>>>");

            String foundBKP = matcher.group().toUpperCase();
            foundBKP = foundBKP
                    .replaceAll("G", "6")
                    .replaceAll("I", "1")
                    .replaceAll("J", "1")
                    .replaceAll("O", "0")
                    .replaceAll("Q", "0")
                    .replaceAll("S", "5")
                    .replaceAll("T", "7")
                    .replaceAll("Z", "2");

            return foundBKP;
        }
        return null;
    }

    /**
     * Kiwi, to nie je dick
     * @param block content
     * @return vatID
     */
    @Nullable
    private String getDIC(@NonNull FirebaseVisionText.TextBlock block) {

        Matcher matcher = ext.get("DIC").matcher(block.getText());
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;

    }

    @Nullable
    private Boolean getMode(@NonNull FirebaseVisionText.TextBlock block) {

        Boolean isSimple = null;
        Matcher matcher;

        matcher = ext.get("MODE_STD").matcher(block.getText());
        if (matcher.find())
            isSimple = false;

        matcher = ext.get("MODE_EZ").matcher(block.getText());
        if (matcher.find())
            isSimple = true;

        if (isSimple == null)
            return null;

        return isSimple;
    }

    @Nullable
    private String getDate(@NonNull FirebaseVisionText.TextBlock block) {

        String result;
        String day, month, year;
        Matcher matcher;

        // finding format: "DD.MM.YYYY"
        matcher = ext.get("DATE_BASE").matcher(block.getText());
        if (matcher.find()) {
            // found format: "D-DD.M-MM.YYYY"
            result = matcher.group();

            // Step 1: get day
            matcher = ext.get("DATE_DAY").matcher(result);
            if (matcher.find()) {
                day = matcher.group();
            }
            else {
                // didn't find DD.?.?, lookup for D.?.?
                matcher = ext.get("DATE_DAY_SHORT").matcher(result);
                if (matcher.find()) {
                    day = "0" + matcher.group();
                }
                else return null; // even D.MM.YYYY not found, invalid date
            }

            // Step 2: get month
            matcher = ext.get("DATE_MONTH").matcher(result);
            if (matcher.find()) {
                month = matcher.group().substring(0, 2);
            }
            else {
                // ?.MM.YYYY not found
                matcher = ext.get("DATE_MONTH_SHORT").matcher(result);
                if (matcher.find()) {
                    month = "0" + matcher.group().substring(1, 2);
                }
                else return null; // ?.M.YYYY not found
            }

            // Step 3: get year
            matcher = ext.get("DATE_YEAR").matcher(result);
            if (!matcher.find()) {
                Log.wtf(TAG, "Error: extract(): 'DATE': yearPattern.matcher() found nothing (original string)->[" + result + "]");
                return null;
            }
            year = matcher.group();
            int y = Integer.valueOf(year);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (! (y <= currentYear && y >= currentYear - 1))
                return null;

            String finalDate = year + "-" + month + "-" + day;
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                df.setLenient(false);
                df.parse(finalDate);
            }
            catch (ParseException e) {
                return null;
            }
            return finalDate;
        }
        else {
            // Format1 not found! Finding format: "DD.MM.YY"
            matcher = ext.get("DATE_SHORT").matcher(block.getText());
            if (matcher.find()) {
                // Found format: "DD.MM.YY"
                result = matcher.group();
                matcher = ext.get("DATE_DAY").matcher(result);
                if (!matcher.find())
                    return null;
                day = matcher.group();

                matcher = ext.get("DATE_MONTH").matcher(result);
                if (!matcher.find())
                    return null;
                month = matcher.group().substring(0, 2);

                matcher = ext.get("DATE_YEAR_SHORT").matcher(result);
                if (!matcher.find())
                    return null;

                year = "20" + matcher.group();

                int y = Integer.valueOf(year);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                if (! (y <= currentYear && y >= currentYear - 1))
                    return null;

                String finalDate = year + "-" + month + "-" + day;
                try {
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                    df.setLenient(false);
                    df.parse(finalDate);
                }
                catch (ParseException e) {
                    return null;
                }
                return finalDate;
            }
            else {
                // Even second date format was not found, bye bye
                return null;
            }

        }

    }

    @Nullable
    private String getTime(@NonNull FirebaseVisionText.TextBlock block) {

        Matcher matcher = ext.get("TIME").matcher(block.getText());

        if (matcher.find()) {
            String result = matcher.group();
            String hour, minute;

            matcher = ext.get("TIME_HRS").matcher(result);
            if (!matcher.find())
                return null;

            hour = matcher.group();

            // Cut out the unnecessary part of string
            result = result.substring(matcher.end());
            matcher = ext.get("TIME_MIN").matcher(result);
            if (!matcher.find())
                return null;

            minute = matcher.group().substring(1);

            if (!(0 <= Integer.valueOf(hour) && Integer.valueOf(hour) <= 23))
                return null;
            if (!(0 <= Integer.valueOf(minute) && Integer.valueOf(minute) <= 59))
                return null;

            return hour + ":" + minute;
        }
        return null;
    }

    @Nullable
    private Map<Coords, Float> getSums(@NonNull FirebaseVisionText.TextBlock block) {

        Map<Coords, Float> map = new HashMap<>();
        Pattern regex = base.get(Entry.SUM);
        Matcher matcher;

        for (FirebaseVisionText.Line line : block.getLines()) {
            if (line.getBoundingBox() == null)
                continue;

            matcher = regex.matcher(line.getText());
            if (matcher.find()) {
                String found = matcher.group();

                Matcher intpartMatcher = ext.get("SUM_INTPART").matcher(found);
                if (!intpartMatcher.find()) {
                    Log.wtf(TAG, "Error: extract(): 'SUM': 'SUM_INTPART'.matcher() found nothing (original string)->[" + found + "]");
                    return null;
                }

                Matcher decpartMatcher = ext.get("SUM_DECPART").matcher(found.substring(intpartMatcher.end()));
                if (!decpartMatcher.find()) {
                    Log.wtf(TAG, "Error: extract(): 'SUM': 'SUM_DECPART'.matcher() found nothing (original string)->[" + found + "]");
                    return null;
                }

                String decPart = decpartMatcher.group();
                if (decPart.length() == 1) {
                    decPart += "0";
                }

                float price = Float.parseFloat(intpartMatcher.group()) +
                                Float.parseFloat(decPart) / 100;

                Coords loc = new Coords(
                    line.getBoundingBox().left,
                    line.getBoundingBox().bottom,
                    line.getBoundingBox().top
                );

                map.put(loc, price);

            }
        }

        return map.size() > 0 ? map : null;
    }

    /**
     * We will see if this works...
     * @param block content
     * @return list of coordinates of words recognized as sum-words
     */
    @Nullable
    private List<Coords> getWords(@NonNull FirebaseVisionText.TextBlock block) {

        List<Coords> foundWords = new ArrayList<>();
        Pattern regex = base.get(Entry.WORD);
        Matcher matcher;

        for (FirebaseVisionText.Line line : block.getLines()) {
            for (FirebaseVisionText.Element e : line.getElements()) {
                matcher = regex.matcher(e.getText());
                if (matcher.find()) {

                    if (e.getBoundingBox() != null) {
                        foundWords.add(new Coords(
                                e.getBoundingBox().left,
                                e.getBoundingBox().bottom,
                                e.getBoundingBox().top
                        ));
                    }
                    else {
                        Log.wtf(TAG, "getWords :: boundingbox was null?");
                    }

                }
            }
        }
        return (foundWords.size() > 0) ? foundWords : null;
    }

}
