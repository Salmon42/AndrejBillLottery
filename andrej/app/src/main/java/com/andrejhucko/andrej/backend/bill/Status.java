package com.andrejhucko.andrej.backend.bill;

import com.andrejhucko.andrej.R;

public enum Status {

    NEW             ("NEW",         R.string.bill_detail_state_new),
    VERIFIED        ("VERIFIED",    R.string.bill_detail_state_verified),
    IN_DRAW         ("IN_DRAW",     R.string.bill_detail_state_indraw),
    NOT_WINNING     ("NOT_WINNING", R.string.bill_detail_state_not_winning),
    WINNING         ("WINNING",     R.string.bill_detail_state_winning),
    VIO_1x1x1       ("VIO_1x1x1",   R.string.bill_detail_state_vio),

    // for intern storage bills for pairing
    NOT_REGISTERED  ("NOT_REGISTERED",  R.string.bill_detail_state_not_reg),
    // ---

    REJECTED        ("REJECTED",    R.string.epsilon),
    OUTDATED        ("OUTDATED",    R.string.epsilon),
    UNHANDLED       ("UNHANDLED",   R.string.epsilon),

    // DUPLICATE:
    // In context of LotteryConnection :: registerBill the bill is discarded,
    // in context of LotteryConnection :: getBills it means VIO_1x1x1
    DUPLICATE       ("DUPLICATE",   R.string.bill_detail_state_vio);

    final String status;
    final int printId;
    Status(String status, int id) {
        this.status = status;
        this.printId = id;
    }
    public String n() {
        return status;
    }

    public int print() {
        return printId;
    }

    public static Status get(String status) {
        for (Status s : Status.values()) {
            if (s.n().equals(status)) return s;
        }
        return UNHANDLED;
    }
}
