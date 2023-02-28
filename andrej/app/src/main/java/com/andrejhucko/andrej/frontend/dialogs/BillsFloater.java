package com.andrejhucko.andrej.frontend.dialogs;

import java.util.List;
import android.content.Context;
import android.support.v7.widget.*;
import android.view.LayoutInflater;
import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.bill.Bill;

public class BillsFloater {

    private final LazyAlertDialog dialog;
    private final LazyRecyclerView view;
    private final RecyclerViewAdapter.Listener recyclerListener;

    public interface Listener {
        void popDetail(Bill bill);
    }

    public BillsFloater(final Listener listener) {
        view = new LazyRecyclerView();
        dialog = new LazyAlertDialog();
        recyclerListener = new RecyclerViewAdapter.Listener() {
            @Override
            public void popDetail(Bill bill) {
                if (listener != null) listener.popDetail(bill);
            }
        };
    }

    @SuppressLint("InflateParams")
    public void show(Context context, List<Bill> bills) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppRoundDialog);
        view.set((RecyclerView) LayoutInflater.from(context).inflate(R.layout.frag__lottery_float_list, null));
        dialog.set(builder.setView(view.get()).create());

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(context, bills, recyclerListener);
        view.get().setAdapter(adapter);
        view.get().setLayoutManager(new LinearLayoutManager(context));

        dialog.get().show();
    }

    public static void hide(BillsFloater floater) {
        if (floater != null) {
            floater.dialog.dismiss();
        }
    }
}
