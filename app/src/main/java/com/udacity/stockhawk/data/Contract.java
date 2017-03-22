package com.udacity.stockhawk.data;


import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.collect.ImmutableList;

public final class Contract {

    static final String AUTHORITY = "com.udacity.stockhawk";
    static final String PATH_QUOTE = "quote";
    static final String PATH_QUOTE_WITH_SYMBOL = "quote/*";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private Contract() {
    }

    @SuppressWarnings("unused")
    public static final class Quote implements BaseColumns {

        public static final Uri URI = BASE_URI.buildUpon().appendPath(PATH_QUOTE).build();

        public static final String TABLE_NAME = "quotes";

        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_ABSOLUTE_CHANGE = "absolute_change";
        public static final String COLUMN_PERCENTAGE_CHANGE = "percentage_change";
        public static final String COLUMN_MONTH_HISTORY = "month_history";
        public static final String COLUMN_STOCK_EXCHANGE = "stock_exchange";
        public static final String COLUMN_STOCK_NAME = "stock_name";
        public static final String COLUMN_WEEK_HISTORY = "week_history";
        public static final String COLUMN_DAY_HISTORY = "day_history";
        public static final String COLUMN_DAY_HIGHEST = "day_highest";
        public static final String COLUMN_DAY_LOWEST = "day_lowest";

        public static final int POSITION_ID = 0;
        public static final int POSITION_SYMBOL = 1;
        public static final int POSITION_PRICE = 2;
        public static final int POSITION_ABSOLUTE_CHANGE = 3;
        public static final int POSITION_PERCENTAGE_CHANGE = 4;
        public static final int POSITION_MONTH_HISTORY = 5;
        public static final int POSITION_WEEK_HISTORY = 6;
        public static final int POSITION_DAY_HISTORY = 7;
//        public static final int POSITION_EXCHANGE = 8;
        public static final int POSITION_NAME = 8;
        public static final int POSITION_LOWEST = 9;
        public static final int POSITION_HIGHEST = 10;

        public static final ImmutableList<String> QUOTE_COLUMNS = ImmutableList.of(
                _ID,
                COLUMN_SYMBOL,
                COLUMN_PRICE,
                COLUMN_ABSOLUTE_CHANGE,
                COLUMN_PERCENTAGE_CHANGE,
                COLUMN_MONTH_HISTORY,
                COLUMN_WEEK_HISTORY,
                COLUMN_DAY_HISTORY,
//                COLUMN_STOCK_EXCHANGE,
                COLUMN_STOCK_NAME,
                COLUMN_DAY_LOWEST,
                COLUMN_DAY_HIGHEST
        );

        public static Uri makeUriForStock(String symbol) {
            return URI.buildUpon().appendPath(symbol).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }


    }

}
