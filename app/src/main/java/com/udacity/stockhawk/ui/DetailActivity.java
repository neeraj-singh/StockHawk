package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PAGE_LIMIT = 2;
    private static int LOADER_ID = 0;
    public Boolean dataLoaded = false;
    @BindView(R.id.toolbar)
    public Toolbar toolbar;
    @BindView(R.id.current_rate)
    public TextView tvStockPrice;
    @BindView(R.id.highest_rate)
    public TextView tvDayHighest;
    @BindView(R.id.lowest_rate)
    public TextView tvDayLowest;
    @BindView(R.id.absolute_change)
    public TextView tvAbsoluteChange;
    @BindView(R.id.viewpager)
    public ViewPager viewPager;
    @BindView(R.id.tabs)
    public TabLayout tabLayout;
    private Uri stockUri;

    ActionBar supportActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportPostponeEnterTransition();
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        stockUri = intent == null ? null : intent.getData();

        setupViewPager();

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager() {

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        Bundle bundle = new Bundle();
        DetailFragment monthlyStock = new DetailFragment();
        bundle.putString(getString(R.string.FRAGMENT_DATA_TYPE_KEY), getString(R.string.MONTHLY));
        monthlyStock.setArguments(bundle);

        DetailFragment weeklyStock = new DetailFragment();
        bundle = new Bundle();
        bundle.putString(getString(R.string.FRAGMENT_DATA_TYPE_KEY), getString(R.string.WEEKLY));
        weeklyStock.setArguments(bundle);

        DetailFragment dailyStock = new DetailFragment();
        bundle = new Bundle();
        bundle.putString(getString(R.string.FRAGMENT_DATA_TYPE_KEY), getString(R.string.DAILY));
        dailyStock.setArguments(bundle);

        viewPagerAdapter.addFragment(dailyStock, getString(R.string.days_fragment_title));
        viewPagerAdapter.addFragment(weeklyStock, getString(R.string.weeks_fragment_title));
        viewPagerAdapter.addFragment(monthlyStock, getString(R.string.months_fragment_title));

        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(PAGE_LIMIT);
        tabLayout.setupWithViewPager(viewPager, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (stockUri != null) {
            return new CursorLoader(
                    this,
                    stockUri,
                    Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            String stockName = data.getString(Contract.Quote.POSITION_NAME);
            Float stockPrice = data.getFloat(Contract.Quote.POSITION_PRICE);
            Float absolutionChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            Float dayLowest = data.getFloat(Contract.Quote.POSITION_LOWEST);
            Float dayHighest = data.getFloat(Contract.Quote.POSITION_HIGHEST);

            getWindow().getDecorView().setContentDescription(
                    String.format(getString(R.string.detail_activity_cd), stockName));

            DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.getDefault());
            dollarFormat.setMaximumFractionDigits(2);
            dollarFormat.setMinimumFractionDigits(2);

            supportActionBar.setTitle(stockName);
            tvStockPrice.setText(dollarFormat.format(stockPrice));
            tvStockPrice.setContentDescription(String.format(getString(R.string.stock_price_cd), tvStockPrice.getText()));
            tvAbsoluteChange.setText(dollarFormat.format(absolutionChange));
            if (dayHighest != -1) {
                tvDayHighest.setText(dollarFormat.format(dayHighest));
                tvDayHighest.setContentDescription(String.format(getString(R.string.day_highest_cd), tvDayHighest.getText()));
                tvDayLowest.setText(dollarFormat.format(dayLowest));
                tvDayLowest.setContentDescription(String.format(getString(R.string.day_lowest_cd), tvDayLowest.getText()));
            } else {
                tvDayLowest.setVisibility(View.GONE);
                tvDayHighest.setVisibility(View.GONE);
            }
            if (absolutionChange > 0) {
                tvAbsoluteChange.setBackgroundResource(R.drawable.percent_change_pill_green);
                tvAbsoluteChange.setContentDescription(
                        String.format(getString(R.string.stock_increment_cd), tvAbsoluteChange.getText()));
            } else {
                tvAbsoluteChange.setBackgroundResource(R.drawable.percent_change_pill_red);
                tvAbsoluteChange.setContentDescription(
                        String.format(getString(R.string.stock_decrement_cd), tvAbsoluteChange.getText()));
            }
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        dataLoaded = false;
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }
    }
}
