package com.andrejhucko.andrej.frontend.dialogs.filter;

import java.util.*;
import android.view.*;
import android.widget.*;
import android.content.Context;
import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import android.support.design.widget.TabLayout;
import java.util.concurrent.atomic.AtomicBoolean;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.utility.App;
import com.andrejhucko.andrej.backend.utility.DefStorage;
import com.andrejhucko.andrej.backend.utility.DrawDate;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.utility.Storage;

public class BillFilter {

    private final LazyAlertDialog dialog;
    private final LazyView view;
    private BillFilterChoice choices;
    private final AtomicBoolean bool;

    public interface Listener {
        //void onItemSelected(int itemId);
        void onFilterPick(BillFilterChoice choice);
    }

    public BillFilter() {
        choices = new BillFilterChoice();
        dialog = new LazyAlertDialog();
        view = new LazyView();
        bool = new AtomicBoolean(false);
    }

    /**
     * Just sweet little wrapper
     * @param id  R.id
     * @param <T> subclass of {@link View}
     * @return View
     */
    private <T extends View> T getView(int id) {
        return view.get().findViewById(id);
    }

    @SuppressLint("InflateParams")
    public void show(final Context context, final Listener listener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppRoundDialog);
        view.set(LayoutInflater.from(context).inflate(R.layout.frag__my_bills_filter, null));
        dialog.set(builder.setView(view.get()).create());

        DefStorage store = Storage.def(context);

        final RelativeLayout[] layouts = {
                getView(R.id.filter_bill_sublayout_0),
                getView(R.id.filter_bill_sublayout_1)
        };

        choices = new BillFilterChoice(store.getBillFilter());

        final RadioGroup drawGroup = getView(R.id.filter_bill_group_draws);
        final RadioGroup pickGroup = getView(R.id.filter_bill_group_pick);

        // ----

        List<String> draws = new ArrayList<>();
        for (String user : store.getAccList()) draws.addAll(store.getDraws(user));

        Collections.sort(draws, Collections.<String>reverseOrder());

        draws.remove(DrawDate.getNextLotteryDate());
        for (String d : draws) addToGroup(context, drawGroup, d);

        // ----

        radioCheck(drawGroup, choices.getDraw());
        radioCheck(pickGroup, choices.getPick());

        final TabLayout tabs = getView(R.id.filter_bill_layout_tab);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int other = 1 - tab.getPosition();
                layouts[tab.getPosition()].setVisibility(View.VISIBLE);
                layouts[other].setVisibility(View.INVISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        drawGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                choices.setDraw((String) group.findViewById(checkedId).getTag());

                if (group.findViewById(checkedId).getTag().equals(DefStorage.BILL_FILTER.D_UNSENT)) {
                    listener.onFilterPick(choices);
                    dialog.dismiss();
                }

                layouts[1].setVisibility(View.VISIBLE);
                layouts[0].setVisibility(View.INVISIBLE);
                TabLayout.Tab t = tabs.getTabAt(1);
                if (t != null) t.select();
                bool.set(true);
                pickGroup.clearCheck();
            }
        });

        pickGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == -1) return;
                if (bool.get()) {
                    bool.set(false);
                    return;
                }

                choices.setPick((String) group.findViewById(checkedId).getTag());
                listener.onFilterPick(choices);
                dialog.dismiss();
            }
        });

        dialog.get().show();
    }

    public static void hide(BillFilter filter) {
        if (filter != null) {
            filter.dialog.dismiss();
        }
    }

    private View findRadioBtnByTag(RadioGroup group, String tag) {
        for (int c = 0; c < group.getChildCount(); c++) {
            if (group.getChildAt(c).getTag().equals(tag)) {
                return group.getChildAt(c);
            }
        }
        return null;
    }

    private void radioCheck(RadioGroup group, String tag) {
        View v = findRadioBtnByTag(group, tag);
        if (v != null) {
            group.check(v.getId());
        }
    }

    @SuppressLint("InflateParams")
    private void addToGroup(Context context, RadioGroup parent, String tag) {

        // Inflate view
        LazyView view = new LazyView();
        view.set(LayoutInflater.from(context).inflate(R.layout.frag__my_bills_filter_item, null));

        // Set title and tag
        ((RadioButton) view.get()).setText(DrawDate.print(context, new DrawDate(tag)));
        view.get().setTag(tag);

        // .findViewById(R.id.filter_bill_draw_item)

        // Add the view to parent (radiogroup)
        parent.addView(view.get());
        view.set(parent.getChildAt(parent.getChildCount() - 1));

        // Set correct layoutparams
        RadioGroup.LayoutParams params = (RadioGroup.LayoutParams) view.get().getLayoutParams();
        params.width = LinearLayout.LayoutParams.MATCH_PARENT;

        int px = App.dpToPixel(context, 8);
        params.setMargins(px, px, px, px);

        view.get().setLayoutParams(params);

    }

}
