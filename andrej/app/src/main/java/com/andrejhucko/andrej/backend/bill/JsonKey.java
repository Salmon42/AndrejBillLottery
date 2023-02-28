package com.andrejhucko.andrej.backend.bill;

/** Usage: Uctenkovka responses with JSON, these are the keys to the values */
public enum JsonKey {

    ID          ("id"),                     // INT
    FIK         ("fik"),                    // STR: 1234 5678 - 1234 - 1234
    BKP         ("bkp"),                    // STR: 12345678 - 87654321
    DATE        ("date"),                   // STR: YYYY-MM-DD
    TIME        ("time"),                   // STR: HH:MM:SS
    AMOUNT      ("amount"),                 // INT: 12100
    MODE        ("simpleMode"),             // BOOL
    VAT_ID      ("vatId"),                  // STR
    STATUS      ("status"),                 // STR: SOME_CODE
    REG_TS      ("registrationDateTime"),   // STR: YYYY-MM-DDTHH:MM:SS.ss+hh:mm
    DRAW_DATE   ("drawDate"),               // STR: YYYY-MM-DD
    PLAYER      ("playerId"),               // INT: 123456
    // ...
    PHONE       ("phone"),                  // only in registration form
    R_ID        ("receiptId"),
    R_STATUS    ("receiptStatus"),          // only in received JSON, basically duplicate with STATUS
//  CHANNEL     ("channel"),                // I have no idea why do they have this

    // Winnings-themed keys
    W_PRIZE     ("winDescription"),         // Winning bill: [String] "100 Kč"
    W_PAYSTAT   ("winPaymentStatus");       // [String] - caps-lock-value

    // Sheet for W_PAYSTAT:
    // ? ? ? ? ? ?  : Čeká na vyplacení
    // ? ? ? ? ? ?  : Čeká na předání (auto)
    // BACC_SUCCESS : Vyplaceno převodem
    // ? ? ? ? ? ?  : Vyplaceno hotově
    // ? ? ? ? ? ?  : Předáno (auto)
    // ? ? ? ? ? ?  : Čeká na ověření mojeID
    // ? ? ? ? ? ?  : Chybné číslo účtu
    // ? ? ? ? ? ?  : Nevyplněno číslo dokladu
    // ? ? ? ? ? ?  : Změňte způsob výplaty
    // ? ? ? ? ? ?  : Právo na výhru nevzniklo
    // ? ? ? ? ? ?  : Právo na výhru zaniklo

    private final String key;
    JsonKey(String key) {
        this.key = key;
    }

    public String n() {
        return key;
    }
}
