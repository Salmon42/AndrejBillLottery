package com.andrejhucko.andrej.fragments;

import android.support.annotation.NonNull;

import com.andrejhucko.andrej.R;

/**
 * Just for sake of improved readability.
 * Correct order of fragments in navigation.
 */
public enum FragInfo {
    Home        (0, "HomeFrag"),
    AddBill     (1, "AddBillFrag"),
    MyBills     (2, "MyBillsFrag"),
    Lottery     (3, "LotteryFrag"),
    Account     (4, "AccountFrag"),
    Settings    (5, "SettingsFrag"),
    BugReport   (6, "BugReportFrag"),
    Donate      (7, "DonateFrag"),
    About       (8, "AboutFrag");

    private final int index;
    private final String name;

    FragInfo(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /** Fragment position index */
    public int pos() {
        return index;
    }

    @NonNull
    public final String getName() {
        return name;
    }

    @NonNull
    public static FragInfo convert(int index) {
        switch (index) {
            case 0: return FragInfo.Home;
            case 1: return FragInfo.AddBill;
            case 2: return FragInfo.MyBills;
            case 3: return FragInfo.Lottery;
            case 4: return FragInfo.Account;
            case 5: return FragInfo.Settings;
            case 6: return FragInfo.BugReport;
            case 7: return FragInfo.Donate;
            case 8: return FragInfo.About;
            default: return FragInfo.Home;
        }
    }

    @NonNull
    public static FragInfo getById(int id) {
        switch (id) {
            case R.id.nav_home:      return FragInfo.Home;
            case R.id.nav_add_bill:  return FragInfo.AddBill;
            case R.id.nav_my_bills:  return FragInfo.MyBills;
            case R.id.nav_lottery:   return FragInfo.Lottery;
            case R.id.nav_account:   return FragInfo.Account;
            case R.id.nav_settings:  return FragInfo.Settings;
            case R.id.nav_bugreport: return FragInfo.BugReport;
            case R.id.nav_donate:    return FragInfo.Donate;
            case R.id.nav_about:     return FragInfo.About;
            default: return FragInfo.Home;
        }
    }
}
