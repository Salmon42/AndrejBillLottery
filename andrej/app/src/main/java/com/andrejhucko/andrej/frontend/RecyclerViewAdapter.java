package com.andrejhucko.andrej.frontend;

import android.view.*;
import java.util.List;
import android.widget.*;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v4.content.ContextCompat;
import android.graphics.drawable.GradientDrawable;
import com.andrejhucko.andrej.R;
import com.andrejhucko.andrej.backend.bill.Bill;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    final private Context context;
    final private List<Bill> contents;
    final private Listener listener;

    /**
     * Inner class for single item of view list.
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout layout;
        View indicatorView;
        TextView dateView;
        TextView timeView;
        TextView contentView;

        ViewHolder(View view) {
            super(view);
            layout = view.findViewById(R.id.fmb_item_layout);
            indicatorView = view.findViewById(R.id.fmb_indicator);

            dateView = view.findViewById(R.id.fmb_billitem_date);
            timeView = view.findViewById(R.id.fmb_billitem_time);
            contentView = view.findViewById(R.id.fmb_billitem_cont);
        }

    }

    public interface Listener {
        void popDetail(Bill bill);
    }

    public RecyclerViewAdapter(Context context, List<Bill> contents, Listener listener) {
        this.context = context;
        this.contents = contents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.frag__my_bills_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        // Set appropriate color
        GradientDrawable d = (GradientDrawable) holder.indicatorView.getBackground();
        switch (contents.get(position).getStatus()) {

            case IN_DRAW:   // v slosovani
            case VERIFIED:  // overena v EET
            case NEW:       // overuje se
                d.setColor(ContextCompat.getColor(context, R.color.billInDraw));
                break;

            case NOT_REGISTERED:
                d.setColor(ContextCompat.getColor(context, R.color.billInCheck));
                break;

            case NOT_WINNING:
                d.setColor(ContextCompat.getColor(context, R.color.billNotWinning));
                break;

            case WINNING:
                d.setColor(ContextCompat.getColor(context, R.color.billIsWinning));
                break;

            default:
                d.setColor(ContextCompat.getColor(context, R.color.billNotRegistered));
                break;
        }

        holder.dateView.setText(contents.get(position).dispRegDate());
        holder.timeView.setText(contents.get(position).dispRegTime());
        holder.contentView.setText(context.getString(R.string.bill_detail_billdate, contents.get(position).dispDate()));

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.popDetail(contents.get(holder.getAdapterPosition()));
            }
        });

    }

    @Override
    public int getItemCount() {
        return contents.size();
    }


    public static void notify(RecyclerViewAdapter adapter) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

}
