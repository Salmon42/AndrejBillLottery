package com.andrejhucko.andrej;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Application;
import android.arch.lifecycle.*;
import android.support.v4.app.Fragment;
import com.andrejhucko.andrej.fragments.*;
import com.andrejhucko.andrej.backend.utility.*;

public final class Andrej extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        storage = new Storage(this);
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Storages
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    private Storage storage; // Preference storage points
    private LoginDlgStorage loginDialogStorage = new LoginDlgStorage(); // for screen rotation
    // Lottery fragment - for calendar scrolling. rotating destroys current state
    private CalendarScrollStorage lotteryFragCalendarScroll = new CalendarScrollStorage();

    /** Container of SharedPreferences */
    public Storage st() {
        return storage;
    }

    /** Storage for unfinished dialog credentials structure from logindialog */
    public LoginDlgStorage lds() {
        return loginDialogStorage;
    }

    /** Lottery Fragment Calendar Scroll */
    public CalendarScrollStorage css() {
        return lotteryFragCalendarScroll;
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Lifecycle
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    private ArrayDeque<Integer> pendingFragments = new ArrayDeque<>();
    private boolean pendingFragmentReload = false;
    private Lifecycle.Event lastBaseActivityEvent = null;
    private Lifecycle.Event lastScanActivityEvent = null;

    public void addPendingFragment(int fragmentID) {
        pendingFragments.add(fragmentID);
    }

    public ArrayDeque<Integer> pendingFragments() {
        ArrayDeque<Integer> returning = pendingFragments;
        pendingFragments = new ArrayDeque<>();
        return returning;
    }

    public void setPendingReload() {
        pendingFragmentReload = true;
    }

    public boolean isPendingFragmentReload() {
        boolean returning = pendingFragmentReload;
        pendingFragmentReload = false;
        return returning;
    }

    /** set life cycle observer for base activity. */
    public void setLCOBase(final Lifecycle lifecycle) {
        lifecycle.addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            void onDestroy(LifecycleOwner source) {
                source.getLifecycle().removeObserver(this);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            void onAny(LifecycleOwner source, Lifecycle.Event event) {
                lastBaseActivityEvent = event;
            }
        });
    }

    /** set life cycle observer for scan activity. */
    public void setLCOScan(final Lifecycle lifecycle) {
        lifecycle.addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            void onDestroy(LifecycleOwner source) {
                source.getLifecycle().removeObserver(this);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            void onAny(LifecycleOwner source, Lifecycle.Event event) {
                lastScanActivityEvent = event;
            }
        });
    }

    public Lifecycle.Event baseEvent() {
        return lastBaseActivityEvent;
    }

    public Lifecycle.Event scanEvent() {
        return lastScanActivityEvent;
    }

    public boolean baseMayShowUp() {
        return lastBaseActivityEvent == Lifecycle.Event.ON_RESUME;
    }

    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  Fragments
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    private ArrayList<Integer> fragmentBackStack = new ArrayList<>();
    private final BaseFragment[] fragments = new BaseFragment[9];
    private BaseFragment currentFragment = null;

    public void updateFragments(List<Fragment> fragments) {

        int oldIndex = (currentFragment == null)
                ? FragInfo.Home.pos()
                : currentFragment.fragmentIndex;

        for (Fragment fragment : fragments) {
            // Iterate through all fragments and reassign new instances of fragments
            this.fragments[((BaseFragment) fragment).getIndex()] = (BaseFragment) fragment;
        }
        currentFragment = this.fragments[oldIndex];

    }

    /**
     * Get referrence to current fragment
     * @return current shown fragment in BaseActivity
     */
    public BaseFragment currentFrag() {
        return currentFragment;
    }

    public BaseFragment getFrag(int index) {
        return fragments[index];
    }

    /**
     * Calls setCurrentFrag(int)
     * @param index FragInfo enum
     * @return whether new fragment has been initialized
     */
    public boolean setCurrentFrag(FragInfo index) {
        if (setCurrentFrag(index.pos())) {
            setFragmentTitle(index);
            return true;
        }
        return false;
    }

    /**
     * Sets current fragment & creates new instance of fragment, if needed
     * @param index index to array of fragments
     * @return whether new fragment has been initialized
     */
    public boolean setCurrentFrag(int index) {
        // Lazy init
        boolean wasInitialized = false;
        if (fragments[index] == null) {
            switch (index) {
                case 0: fragments[0] = new HomeFrag();      break;
                case 1: fragments[1] = new AddBillFrag();   break;
                case 2: fragments[2] = new MyBillsFrag();   break;
                case 3: fragments[3] = new LotteryFrag();   break;
                case 4: fragments[4] = new AccountFrag();   break;
                case 5: fragments[5] = new SettingsFrag();  break;
                case 6: fragments[6] = new BugReportFrag(); break;
                case 7: fragments[7] = new DonateFrag();    break;
                case 8: fragments[8] = new AboutFrag();     break;
                default: break;
            }
            wasInitialized = true;
        }
        this.currentFragment = fragments[index];
        return wasInitialized;
    }

    public void setFragmentTitle(FragInfo info) {
        switch (info) {
            case Home:      fragments[0].setTitle(getString(R.string.app_name));        break;
            case AddBill:   fragments[1].setTitle(getString(R.string.label_addbill));   break;
            case MyBills:   fragments[2].setTitle(getString(R.string.label_mybills));   break;
            case Lottery:   fragments[3].setTitle(getString(R.string.label_lottery));   break;
            case Account:   fragments[4].setTitle(getString(R.string.label_account));   break;
            case Settings:  fragments[5].setTitle(getString(R.string.label_settings));  break;
            case BugReport: fragments[6].setTitle(getString(R.string.label_bugreport)); break;
            case Donate:    fragments[7].setTitle(getString(R.string.label_donate));    break;
            case About:     fragments[8].setTitle(getString(R.string.label_about));     break;
        }
    }

    public int getBackStackSize() {
        return fragmentBackStack.size();
    }

    public void addToBackStack(FragInfo index) {
        fragmentBackStack.add(index.pos());
    }

    public void addToBackStack(int index) {
        fragmentBackStack.add(index);
    }

    public void removeFromBackStack(Integer index) {
        fragmentBackStack.remove(index);
    }

    public void popBackStack() {
        int popindex = fragmentBackStack.size() - 1;
        if (popindex < 0) return;
        fragmentBackStack.remove(popindex);
    }

    public int getBSEntryFragmentAt(int index) {
        return fragmentBackStack.get(index);
    }

    public int getBackStackTop() {
        return fragmentBackStack.get(fragmentBackStack.size() - 1);
    }


    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---  IntroActivity-related
    // --- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---

    public boolean appShouldUpdate = false;
    public boolean introUpdateDialog = false;
    private AtomicBoolean introUpdate = new AtomicBoolean(true);

    /**
     * Prevents situation in which user rotates screen in introactivity during fetch
     * - onResume gets called twice - and if there's no check - two baseactivities are created
     * @return should introactivity call checkForUpdate?
     */
    public boolean introUpdate() {
        boolean value = introUpdate.get();
        introUpdate.set(false);
        return value;
    }

}
