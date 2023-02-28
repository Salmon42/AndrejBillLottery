package com.andrejhucko.andrej.frontend.dialogs;

import android.view.*;
import android.widget.*;
import android.content.Context;
import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.frontend.*;
import com.andrejhucko.andrej.backend.bill.*;
import com.andrejhucko.andrej.backend.utility.App;

public class BillDetail {

    private final LazyAlertDialog dialog;
    private final LazyView view;
    private ConfirmDialog deleteDlg;

    public BillDetail() {
        view = new LazyView();
        dialog = new LazyAlertDialog();
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

    public interface Listener {
        void register();
        void delete();
    }

    @SuppressLint({"InflateParams", "SetTextI18n"})
    public void show(final Context context, final Bill bill, final Listener listener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppRoundDialog);
        view.set(LayoutInflater.from(context).inflate(R.layout.frag__my_bills_detail, null));
        dialog.set(builder.setView(view.get()).create());

        TextView state = getView(R.id.lbdd_status),
                lottery = getView(R.id.lbdd_lotterydate),
                sum = getView(R.id.lbdd_sum),
                datetime = getView(R.id.lbdd_datetime),
                tvFIK = getView(R.id.lbdd_fik),
                tvBKP = getView(R.id.lbdd_bkp),
                mode = getView(R.id.lbdd_mode),
                account = getView(R.id.lbdd_account),
                dic = getView(R.id.lbdd_dic);

        sum.setText(bill.dispAmount());
        datetime.setText(bill.dispDate() + " " + bill.getTime());
        mode.setText(bill.dispSimpleMode());

        if (bill.getFIK() != null) tvFIK.setText(bill.getFIK());
        else getView(R.id.lbdd_fik_layout).setVisibility(View.GONE);

        if (bill.dispBKP() != null) tvBKP.setText(bill.dispBKP());
        else getView(R.id.lbdd_bkp_layout).setVisibility(View.GONE);

        if (!bill.dispBillDrawDate().equals("")) lottery.setText(bill.dispBillDrawDate());
        else getView(R.id.lbdd_lotterydate_layout).setVisibility(View.GONE);

        if (bill.getVatID() != null) dic.setText(bill.getVatID());
        else getView(R.id.lbdd_dic_layout).setVisibility(View.GONE);

        if (bill.getPlayerID() != null) account.setText(App.findPlayerById(context, bill.getPlayerID()));
        else getView(R.id.lbdd_account_layout).setVisibility(View.GONE);

        Button register = getView(R.id.lbdd_register),
                delete = getView(R.id.lbdd_delete);


        if (bill.getStatus() == Status.NOT_REGISTERED) {
            register.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    listener.register();
                }
            });

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    deleteDlg = new ConfirmDialog(context, R.string.bill_detail_dlg_delete, R.string.bill_detail_dlg_delete_desc);
                    deleteDlg.setPositiveButton(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteDlg.dismiss();
                            dialog.dismiss();
                            listener.delete();
                            deleteDlg = null;
                        }
                    });
                    deleteDlg.setNegativeButton(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteDlg.dismiss();
                            deleteDlg = null;
                        }
                    });
                    deleteDlg.show();

                }
            });
        }
        else {
            register.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
        }

        Status billState = bill.getStatus();
        if (billState == Status.WINNING) {

            String winStatus = bill.getWinStatus();
            String winPrize = bill.getWinPrize();
            if (winPrize != null && winStatus != null) {
                getView(R.id.lbdd_status_layout).setVisibility(View.GONE);
                getView(R.id.lbdd_status_winning_layout).setVisibility(View.VISIBLE);
                TextView[] tvs = {
                        getView(R.id.lbdd_winstat_winsum),
                        getView(R.id.lbdd_winstat_state)
                };
                tvs[0].setText(winPrize);
                tvs[1].setText(App.mapWinStat(winStatus));
            }
            else {
                state.setText(billState.print());
            }
        }
        else {
            state.setText(billState.print());
        }

        dialog.get().show();
    }

    public static void hide(BillDetail detail) {
        if (detail != null) {
            if (detail.deleteDlg != null) {
                detail.deleteDlg.dismiss();
            }
            detail.dialog.dismiss();
        }
    }

}
