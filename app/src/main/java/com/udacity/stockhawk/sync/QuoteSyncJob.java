package com.udacity.stockhawk.sync;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import static android.os.Looper.getMainLooper;

public final class QuoteSyncJob {

    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";

    public static final int STOCK_STATUS_OK = 0;
    public static final int STOCK_STATUS_SERVER_DOWN = 1;
    public static final int STOCK_STATUS_SERVER_INVALID = 2;
    public static final int STOCK_STATUS_UNKNOWN = 3;
    public static final int STOCK_STATUS_INVALID = 4;
    public static final int STOCK_STATUS_EMPTY = 5;
    private static final int ONE_OFF_ID = 2;
    private static final int PERIOD = 3600000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static boolean invalidFlag = false;

    @SuppressLint("BinaryOperationInTimber")
    public static void getQuotes(Context context) {

        Calendar from;
        Calendar to = Calendar.getInstance();
        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            if (stockArray.length == 0) {
                setStockStatus(context, STOCK_STATUS_EMPTY);
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            if (quotes.isEmpty()) {
                setStockStatus(context, STOCK_STATUS_SERVER_DOWN);
                return;
            }

            ArrayList<ContentValues> contentValuesArrayList = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();
                Stock stock = quotes.get(symbol);
                StockQuote quote;
                float change;
                float price;
                float dayLowest;
                float dayHighest;
                float percentChange;
                String stockName;
                try {
                    quote = stock.getQuote();
                    price = quote.getPrice().floatValue();

                    BigDecimal temp = quote.getDayLow();
                    if (temp == null) {
                        dayLowest = -1;
                        dayHighest = -1;
                    } else {
                        dayLowest = temp.floatValue();
                        dayHighest = quote.getDayHigh().floatValue();
                    }

                    change = quote.getChange().floatValue();
                    percentChange = quote.getChangeInPercent().floatValue();
                    stockName = stock.getName();
                } catch (NullPointerException exception) {
                    Timber.e(exception, "Incorrect stock symbol entered : " + symbol);

                    showErrorToast(context, symbol);
                    PrefUtils.removeStock(context, symbol);
                    if (PrefUtils.getStocks(context).size() == 0) {
                        setStockStatus(context, STOCK_STATUS_EMPTY);
                    } else {
                        setStockStatus(context, STOCK_STATUS_INVALID);
                    }
                    invalidFlag = true;
                    continue;
                }

                from = Calendar.getInstance();
                from.add(Calendar.MONTH, -4);
                String monthHistory = getHistory(stock, from, to, Interval.MONTHLY);


                from = Calendar.getInstance();
                from.add(Calendar.DAY_OF_YEAR, -35);
                String weekHistory = getHistory(stock, from, to, Interval.WEEKLY);

                from = Calendar.getInstance();
                from.add(Calendar.DAY_OF_YEAR, -5);
                String dayHistory = getHistory(stock, from, to, Interval.DAILY);

                ContentValues contentValues = new ContentValues();
                contentValues.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                contentValues.put(Contract.Quote.COLUMN_PRICE, price);
                contentValues.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                contentValues.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                contentValues.put(Contract.Quote.COLUMN_MONTH_HISTORY, monthHistory);
                contentValues.put(Contract.Quote.COLUMN_DAY_HISTORY, dayHistory);
                contentValues.put(Contract.Quote.COLUMN_WEEK_HISTORY, weekHistory);
                contentValues.put(Contract.Quote.COLUMN_DAY_HIGHEST, dayHighest);
                contentValues.put(Contract.Quote.COLUMN_DAY_LOWEST, dayLowest);
                contentValues.put(Contract.Quote.COLUMN_STOCK_NAME, stockName);
                contentValuesArrayList.add(contentValues);
            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            contentValuesArrayList.toArray(new ContentValues[contentValuesArrayList.size()]));
            if (!invalidFlag) setStockStatus(context, STOCK_STATUS_OK);
            updateWidget(context);
        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
            setStockStatus(context, STOCK_STATUS_SERVER_DOWN);
        } catch (Exception e) {
            Timber.e(e, "Unknown Error");
            setStockStatus(context, STOCK_STATUS_UNKNOWN);
        }
    }

    public static void updateWidget(Context context) {
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
        context.sendBroadcast(dataUpdatedIntent);
    }

    private static void showErrorToast(final Context context, final String symbol) {
        Handler handler = new Handler(getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, String.format(context.getString(R.string.toast_stock_invalid), symbol), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String getHistory(Stock stock, Calendar from, Calendar to, Interval interval) throws IOException {

        List<HistoricalQuote> history = new ArrayList<>();

        //At times, query over 5-7 days history returns very less data at times.
        //hence performing iterative queries until 5 days of data is received.

        if (interval.equals(Interval.DAILY)) {
            while (history.size() < 5) {
                history = stock.getHistory(from, to, interval);
                from.add(Calendar.DAY_OF_YEAR, -1);
            }
        } else {
            history = stock.getHistory(from, to, interval);
        }

        StringBuilder historyBuilder = new StringBuilder();
        for (HistoricalQuote it : history) {
            historyBuilder.append(it.getDate().getTimeInMillis());
            historyBuilder.append(":");
            historyBuilder.append(it.getClose());
            historyBuilder.append("$");
        }
        return historyBuilder.toString();
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");
        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }


    synchronized public static void initialize(final Context context) {
        schedulePeriodic(context);
        syncImmediately(context);
    }

    synchronized public static void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        }
    }

    /**
     * Sets the stock status into shared preference.
     *
     * @param c              Context to get the PreferenceManager from.
     * @param setStockStatus The IntDef value to set
     */
    @SuppressLint("CommitPrefEdits")
    static private void setStockStatus(Context c, @StockStatus int setStockStatus) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(c.getString(R.string.pref_stock_status_key), setStockStatus);
        spe.commit();
    }


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STOCK_STATUS_OK, STOCK_STATUS_SERVER_DOWN, STOCK_STATUS_SERVER_INVALID, STOCK_STATUS_INVALID, STOCK_STATUS_UNKNOWN, STOCK_STATUS_EMPTY})
    public @interface StockStatus {
    }
}
