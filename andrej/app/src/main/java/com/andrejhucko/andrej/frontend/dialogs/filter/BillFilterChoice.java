package com.andrejhucko.andrej.frontend.dialogs.filter;

import android.util.Pair;

public class BillFilterChoice {

    private Pair<String, String> choices;

    public BillFilterChoice() {
        this.choices = new Pair<>(null, null);
    }

    public BillFilterChoice(String drawChoice, String pickChoice) {
        this.choices = new Pair<>(drawChoice, pickChoice);
    }

    BillFilterChoice(BillFilterChoice other) {
        this.choices = other.choices;
    }

    public void setDraw(String drawChoice) {
        this.choices = new Pair<>(drawChoice, this.choices.second);
    }

    public void setPick(String pickChoice) {
        this.choices = new Pair<>(this.choices.first, pickChoice);
    }

    public String getDraw() {
        return this.choices.first;
    }

    public String getPick() {
        return this.choices.second;
    }

}
