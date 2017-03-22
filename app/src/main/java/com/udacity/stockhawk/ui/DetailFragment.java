package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.utils.CustomMarkerView;
import com.udacity.stockhawk.utils.XAxisFormatter;
import com.udacity.stockhawk.utils.YAxisFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public String fragmentDataType;
    public String dateFormat;
    public int dataColumnPosition;
    public Uri stockUri;
    public String historyData;
    public int LOADER_ID;
    @BindView(R.id.chart)
    public LineChart linechart;
    @BindColor(R.color.white)
    public int white;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (savedInstanceState == null) {
            fragmentDataType = getArguments().getString(getString(R.string.FRAGMENT_DATA_TYPE_KEY));
            if (fragmentDataType.equals(getString(R.string.MONTHLY))) {
                dataColumnPosition = Contract.Quote.POSITION_MONTH_HISTORY;
                dateFormat = "MMM";
                LOADER_ID = 100;
            } else if (fragmentDataType.equals(getString(R.string.WEEKLY))) {
                dataColumnPosition = Contract.Quote.POSITION_WEEK_HISTORY;
                dateFormat = "dd";
                LOADER_ID = 200;
            } else {
                dataColumnPosition = Contract.Quote.POSITION_DAY_HISTORY;
                dateFormat = "dd";
                LOADER_ID = 300;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, rootView);
        if (historyData != null) {
            setUpLineChart();
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        stockUri = getActivity().getIntent().getData();
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    private Entry getLastButOneData(List<Entry> dataPairs) {
        if (dataPairs.size() > 2) {
            return dataPairs.get(dataPairs.size() - 2);
        } else {
            return dataPairs.get(dataPairs.size() - 1);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (stockUri != null && getContext() != null) {
            return new CursorLoader(
                    getContext(),
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
        if (historyData == null && data.moveToFirst()) {
            //set up the chart with history data
            historyData = data.getString(dataColumnPosition);
            setUpLineChart();
            getActivity().supportStartPostponedEnterTransition();
        }
    }

    private void setUpLineChart() {
        Pair<Float, List<Entry>> result = getFormattedStockHistory(historyData);
        List<Entry> dataPairs = result.second;
        Float referenceTime = result.first;
        LineDataSet dataSet = new LineDataSet(dataPairs, "");
        dataSet.setColor(white);
        dataSet.setLineWidth(2f);
        dataSet.setDrawHighlightIndicators(false);
        dataSet.setCircleColor(white);
        dataSet.setHighLightColor(white);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        linechart.setData(lineData);

        XAxis xAxis = linechart.getXAxis();
        xAxis.setValueFormatter(new XAxisFormatter(dateFormat, referenceTime));
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineColor(white);
        xAxis.setAxisLineWidth(1.5f);
        xAxis.setTextColor(white);
        xAxis.setTextSize(12f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxisRight = linechart.getAxisRight();
        yAxisRight.setEnabled(false);

        YAxis yAxis = linechart.getAxisLeft();
        yAxis.setValueFormatter(new YAxisFormatter());
        yAxis.setDrawGridLines(false);
        yAxis.setAxisLineColor(white);
        yAxis.setAxisLineWidth(1.5f);
        yAxis.setTextColor(white);
        yAxis.setTextSize(12f);

        CustomMarkerView customMarkerView = new CustomMarkerView(getContext(),
                R.layout.marker_view, getLastButOneData(dataPairs), referenceTime);


        Legend legend = linechart.getLegend();
        legend.setEnabled(false);

        linechart.setMarker(customMarkerView);

        linechart.setDragEnabled(false);
        linechart.setScaleEnabled(false);
        linechart.setDragDecelerationEnabled(false);
        linechart.setPinchZoom(false);
        linechart.setDoubleTapToZoomEnabled(false);
        Description description = new Description();
        description.setText(" ");
        linechart.setDescription(description);
        linechart.setExtraOffsets(10, 0, 0, 10);
        linechart.animateX(1500, Easing.EasingOption.Linear);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //No action needed
    }

    private Pair<Float, List<Entry>> getFormattedStockHistory(String history) {
        List<Entry> entries = new ArrayList<>();
        List<Float> timeData = new ArrayList<>();
        List<Float> stockPrice = new ArrayList<>();
        String[] dataPairs = history.split("\\$");

        for (String pair : dataPairs) {
            String[] entry = pair.split(":");
            timeData.add(Float.valueOf(entry[0]));
            stockPrice.add(Float.valueOf(entry[1]));
        }
        Collections.reverse(timeData);
        Collections.reverse(stockPrice);
        Float referenceTime = timeData.get(0);
        for (int i = 0; i < timeData.size(); i++) {
            entries.add(new Entry(timeData.get(i) - referenceTime, stockPrice.get(i)));
        }
        return new Pair<>(referenceTime, entries);
    }
}
