package com.andrejhucko.andrej.backend.bill;

public enum Entry {
    FIK     (0), // Basic
    TIME    (1), // Basic
    DATE    (2), // Basic
    MODE    (3), // Basic
    SUM     (4), // Basic
    WORD    (5), // Manager-specific
    DIC     (6), // Basic
    BKP     (7); // Manager-specific

    private final int entryIndex;
    Entry(int entry) {
        this.entryIndex = entry;
    }

    public int index() {
        return entryIndex;
    }
}