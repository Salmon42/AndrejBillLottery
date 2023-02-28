package com.andrejhucko.andrej.frontend.dialogs;

public interface AppDialogInterface {

    /**
     * Perform alertDialog show() with inflated view from xml
     */
    void show();

    /**
     * Parent should call whenever it gets destroyed.
     */
    void onParentDestroyed();

}
