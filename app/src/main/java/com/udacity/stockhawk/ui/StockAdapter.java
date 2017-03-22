package com.udacity.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    final private Context context;
    final private DecimalFormat dollarFormat;
    final private DecimalFormat percentageFormat;
    final private StockAdapterOnClickHandler clickHandler;
    private Cursor cursor;

    StockAdapter(Context context, StockAdapterOnClickHandler clickHandler) {
        this.context = context;
        this.clickHandler = clickHandler;

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.getDefault());
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
    }

    public void setCursor(Cursor cursor) {
        if (this.cursor != null) {
            this.cursor.close();
        }
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    public String getSymbolAtPosition(int position) {
        cursor.moveToPosition(position);
        return cursor.getString(Contract.Quote.POSITION_SYMBOL);
    }

    @Override
    public StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(context).inflate(R.layout.list_item_quote, parent, false);

        return new StockViewHolder(item);
    }

    @Override
    public void onBindViewHolder(StockViewHolder holder, int position) {

        if (cursor != null) {
            cursor.moveToPosition(position);

            holder.symbol.setText(cursor.getString(Contract.Quote.POSITION_SYMBOL));
            holder.price.setText(dollarFormat.format(cursor.getFloat(Contract.Quote.POSITION_PRICE)));
            holder.price.setContentDescription(String.format(context.getString(R.string.stock_price_cd), holder.price.getText()));

            float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            String change = dollarFormat.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            if (PrefUtils.getDisplayMode(context)
                    .equals(context.getString(R.string.pref_display_mode_absolute_key))) {
                holder.change.setText(change);
            } else {
                holder.change.setText(percentage);
            }

            if (rawAbsoluteChange > 0) {
                holder.change.setBackgroundResource(R.drawable.percent_change_pill_green);
                holder.change.setContentDescription(
                        String.format(context.getString(R.string.stock_increment_cd), holder.change.getText()));
            } else {
                holder.change.setBackgroundResource(R.drawable.percent_change_pill_red);
                holder.change.setContentDescription(
                        String.format(context.getString(R.string.stock_decrement_cd), holder.change.getText()));
            }
            ViewCompat.setTransitionName(holder.price, context.getString(R.string.stock_price_transition_name) + position);
            ViewCompat.setTransitionName(holder.change, context.getString(R.string.stock_change_transition_name) + position);
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();
        }
        return count;
    }


    interface StockAdapterOnClickHandler {
        void onClick(String symbol, StockViewHolder viewHolder);
    }

    class StockViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.symbol)
        public TextView symbol;
        @BindView(R.id.price)
        public TextView price;
        @BindView(R.id.change)
        public TextView change;

        StockViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition >= 0) {
                cursor.moveToPosition(adapterPosition);
                int symbolColumn = cursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL);
                clickHandler.onClick(cursor.getString(symbolColumn), this);
            }
        }
    }
}
