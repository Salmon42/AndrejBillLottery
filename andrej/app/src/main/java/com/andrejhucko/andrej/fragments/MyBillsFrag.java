package com.andrejhucko.andrej.fragments;

import java.util.*;

import android.app.Activity;
import android.view.*;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.support.v7.widget.*;
import android.support.annotation.*;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.utility.*;
import com.andrejhucko.andrej.frontend.dialogs.*;
import com.andrejhucko.andrej.activities.MBRActivity;
import com.andrejhucko.andrej.backend.network.Response;
import com.andrejhucko.andrej.frontend.dialogs.filter.BillFilter;
import com.andrejhucko.andrej.frontend.dialogs.filter.BillFilterChoice;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_OK;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_BAD_TOKENS;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_NO_CONNECTION;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_NO_UPDATE_NEEDED;
import static com.andrejhucko.andrej.backend.network.LotteryTask.RET_MISSING_CURRENT_ACC;

public final class MyBillsFrag extends BaseFragment {

    private static final String TAG = "MyBillsFrag";
    private static final int REQUEST_BILL_REG = 0;

    private List<Bill> allBills;
    private List<Bill> shownBills;
    private BillFilter filter;
    private BillDetail detail;
    private BillFilter.Listener filterListener;
    private RecyclerViewAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Listener listener;

    public interface Listener {
        void informInvalidUser();
    }

    public MyBillsFrag() {
        super.fragmentName = TAG;
        super.layoutID = R.layout.frag__my_bills;
        super.fragmentIndex = FragInfo.MyBills.pos();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        updateShownBillList(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null || getActivity() == null) return;
        final Context context = getContext();
        final Activity activity = getActivity();

        RecyclerView recyclerView = view.findViewById(R.id.fmb_recyclerview);
        adapter = new RecyclerViewAdapter(getContext(), shownBills, new RecyclerViewAdapter.Listener() {
            @Override
            public void popDetail(final Bill bill) {
                detail = new BillDetail();
                detail.show(getContext(), bill, new BillDetail.Listener() {
                    @Override
                    public void register() {
                        Intent intent = new Intent(getActivity(), MBRActivity.class);
                        intent.putExtra("BILL_FROM_DETAIL", bill.getRegPayload());
                        getActivity().startActivityForResult(intent, REQUEST_BILL_REG);
                    }

                    @Override
                    public void delete() {
                        App.removeBill(getContext(), bill);
                        updateShownBillList(context);
                    }
                });
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        swipeRefreshLayout = view.findViewById(R.id.fmb_swipelayout);
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                networkRefreshList(activity);
            }
        });

        filterListener = new BillFilter.Listener() {
            @Override
            public void onFilterPick(BillFilterChoice choice) {
                Storage.def(getContext()).setBillFilter(choice);
                updateShownBillList(context);
            }
        };

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_bills_frag, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_bills_refresh) {
            swipeRefreshLayout.setRefreshing(true);
            networkRefreshList(getActivity());
        }
        else if (item.getItemId() == R.id.menu_bills_sort) {
            if (filter == null) filter = new BillFilter();
            filter.show(getContext(), filterListener);
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onPause() {
        BillFilter.hide(filter);
        BillDetail.hide(detail);
        super.onPause();
    }

    /**
     * Update list of stored bills into object list that is passed to recyclerviewadapter
     * Update shown bill list according to current filter.
     */
    private void updateShownBillList(Context context) {

        allBills = App.fetchBills(context);
        if (shownBills == null) {
            // call only once! adapter notify changed works only with one instance
            shownBills = new ArrayList<>();
        }

        BillFilterChoice choice = Storage.def(context).getBillFilter();
        shownBills.clear();

        if (choice.getDraw().equals(DefStorage.BILL_FILTER.D_UNSENT)) {
            shownBills.addAll(allBills);
        }
        else {
            switch (choice.getPick()) {
                case DefStorage.BILL_FILTER.P_ALL: shownBills.addAll(allBills); break;
                case DefStorage.BILL_FILTER.P_INDRAW: fill(Status.IN_DRAW); break;
                case DefStorage.BILL_FILTER.P_NONWIN: fill(Status.NOT_WINNING); break;
                case DefStorage.BILL_FILTER.P_WINNING: fill(Status.WINNING); break;
                case DefStorage.BILL_FILTER.P_WRONG:  fill(Status.VIO_1x1x1); break;
            }
        }
        RecyclerViewAdapter.notify(adapter);

    }

    /** Fill the shown-bill-container with right bills */
    private void fill(Status status) {
        for (Bill bill : allBills) {
            if (bill.getStatus() == status) {
                shownBills.add(bill);
            }
        }
    }

    /**
     * Code shortening.
     */
    private void networkRefreshList(final Activity context) {
        NetworkFetch.getDataOnBackground(context, new NetworkFetch.Listener() {
            @Override
            public void onFinish(Response response) {
                swipeRefreshLayout.setRefreshing(false);

                switch (response.code()) {

                    case RET_OK: {
                        AppToast.show(context, R.string.frag_mybills_updated);
                        updateShownBillList(context);
                        break;
                    }

                    case RET_MISSING_CURRENT_ACC: {
                        AppToast.show(context, R.string.frag_mybills_acc_toast);
                        break;
                    }

                    case RET_NO_CONNECTION: {
                        AppToast.show(context, R.string.frag_mybills_neterr_toast);
                        break;
                    }

                    case RET_BAD_TOKENS: {
                        listener.informInvalidUser();
                        break;
                    }

                    case RET_NO_UPDATE_NEEDED: {
                        AppToast.show(context, R.string.frag_mybills_no_update);
                        break;
                    }

                }

            }
        });
    }

}
