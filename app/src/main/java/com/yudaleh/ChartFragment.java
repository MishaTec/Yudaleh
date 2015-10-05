package com.yudaleh;


import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.melnykov.fab.FloatingActionButton;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

// TODO: 05/10/2015 onSaveInstanceState, onRestoreInstanceState
public class ChartFragment extends android.support.v4.app.Fragment {

    private static final int RANGE_BAR_MIN_TICK_COUNT = 2;
    private View mRoot;

    private PieChart mChart;
    private RangeBar rangeBar;
    private TextView maxDebtIndex;
    private TextView minDebtIndex;

    MenuItem editModeMenuItem;
    Contact selectedContact;

    private final ArrayList<Integer> mColors;
    private List<Contact> mDataHeaders;
    private HashMap<String, Integer> mOwnerNamesCount;
    private HashMap<String, Integer> mOwnerNamesCountNoPhone;
    private HashMap<String, List<Debt>> mDataChildren;
    private int mMaxDebtIndex;
    private FloatingActionButton fab;

    public ChartFragment() {
        mColors = new ArrayList<>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            mColors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            mColors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            mColors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            mColors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            mColors.add(c);

        mColors.add(ColorTemplate.getHoloBlue());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }

        View v = inflater.inflate(R.layout.fragment_chart, container, false);

        mChart = (PieChart) v.findViewById(R.id.pie_chart);
        mChart.setDescription("");

        mChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
                editModeMenuItem.setVisible(false);
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });
        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                if (e == null) {
                    return;
                }
                selectedContact = mDataHeaders.get(e.getXIndex());
                editModeMenuItem.setVisible(true);
            }

            @Override
            public void onNothingSelected() {
                // TODO: 10/5/2015 another chart
                editModeMenuItem.setVisible(false);
            }
        });

        fab = (FloatingActionButton) v.findViewById(R.id.fab2);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
                i.putExtra(Debt.KEY_TAB_TAG, getTag());
                startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
            }
        });

        switchFabMode(getResources().getDrawable(R.drawable.ic_mode_edit_white_24dp));

        maxDebtIndex = (TextView) v.findViewById(R.id.max_debt_index);
        minDebtIndex = (TextView) v.findViewById(R.id.min_debt_index);

        rangeBar = (RangeBar) v.findViewById(R.id.rangeBar);

        rangeBar.setBarColor(getResources().getColor(R.color.primary_dark));
        rangeBar.setConnectingLineColor(getResources().getColor(R.color.primary));

        rangeBar.setThumbColorNormal(getResources().getColor(R.color.accent));
        rangeBar.setThumbColorPressed(getResources().getColor(R.color.accent_pressed));

        rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
                rightThumbIndex = Math.min(rightThumbIndex, mMaxDebtIndex);
                minDebtIndex.setText(String.valueOf(leftThumbIndex + 1));
                maxDebtIndex.setText(String.valueOf(rightThumbIndex + 1));
                setData(leftThumbIndex, rightThumbIndex);
            }
        });

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);

        mRoot = v;
        return v;
    }

    private void switchFabMode(final Drawable icon) {
        final ScaleAnimation growAnim = new ScaleAnimation(0.1f, 1.0f, 0.1f, 1.0f);
        final ScaleAnimation shrinkAnim = new ScaleAnimation(1.0f, 0.1f, 1.0f, 0.1f);

        growAnim.setDuration(500);
        shrinkAnim.setDuration(500);

        fab.setAnimation(shrinkAnim);
        shrinkAnim.start();

        shrinkAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab.setImageDrawable(icon);
                fab.setAnimation(growAnim);
                growAnim.start();
            }
        });
        growAnim.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation){}

            @Override
            public void onAnimationRepeat(Animation animation){}

            @Override
            public void onAnimationEnd(Animation animation)
            {
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        try { // TODO: 22/09/2015 only on data change!
            ParseQuery<Debt> mQuery = Debt.getQuery();
            mQuery.whereEqualTo(Debt.KEY_TAB_TAG, getTag());
            mQuery.whereNotEqualTo(Debt.KEY_CURRENCY_POS, Debt.NON_MONEY_DEBT_CURRENCY);
            mQuery.orderByDescending(Debt.KEY_MONEY_AMOUNT);
            mQuery.fromLocalDatastore();
            extractData(mQuery.find());
        } catch (ParseException e) {
            e.printStackTrace();
            // init to prevent null exception
            mDataChildren = new HashMap<>();
            mOwnerNamesCount = new HashMap<>();
            mOwnerNamesCountNoPhone = new HashMap<>();
            mDataHeaders = new ArrayList<>();
        }

        if (mDataHeaders != null) {
            mMaxDebtIndex = mDataHeaders.size() - 1;
        } else {
            mMaxDebtIndex = -1;
        }
        rangeBar.setTickCount(Math.max(mMaxDebtIndex, RANGE_BAR_MIN_TICK_COUNT));
        int rightThumbIndex = Math.min(rangeBar.getRightIndex(), mMaxDebtIndex);
        int leftThumbIndex = rangeBar.getLeftIndex();
        minDebtIndex.setText(String.valueOf(leftThumbIndex + 1));
        maxDebtIndex.setText(String.valueOf(rightThumbIndex + 1));
        setData(leftThumbIndex, rightThumbIndex);
    }

    // TODO: 10/5/2015 use only once, no need to accesses database again
    private void extractData(List<Debt> debts) {
        mDataChildren = new HashMap<>();
        mOwnerNamesCount = new HashMap<>();
        mOwnerNamesCountNoPhone = new HashMap<>();
        mDataHeaders = new ArrayList<>();

        for (Debt debt : debts) {
            String phone = debt.getPhone();
            // TODO: 05/10/2015 dialog for merging non-phone debts
            String name = debt.getOwner();
            Contact contact = new Contact(phone, name);
            String key = contact.getMapKey();

            // TODO: 03/10/2015 update existing adapters
            if (!mDataChildren.containsKey(key)) {
                List<Debt> debtItems = new ArrayList<>();
                debtItems.add(debt);
                mDataChildren.put(key, debtItems);

                mDataHeaders.add(contact);
            } else {
                mDataChildren.get(key).add(debt);
            }

            for (Contact c : mDataHeaders) {
                c.setTotalMoney(countTotalMoney(c.getMapKey()));
            }
            if (phone != null) {
                if (!mOwnerNamesCount.containsKey(name)) {
                    mOwnerNamesCount.put(name, 1);
                } else {
                    mOwnerNamesCount.put(name, mOwnerNamesCount.get(name) + 1);
                }
            } else {
                if (!mOwnerNamesCountNoPhone.containsKey(name)) {
                    mOwnerNamesCountNoPhone.put(name, 1);
                } else {
                    mOwnerNamesCountNoPhone.put(name, mOwnerNamesCountNoPhone.get(name) + 1);
                }
            }
        }
        Collections.sort(mDataHeaders, new Comparator<Contact>() {
            @Override
            public int compare(Contact lhs, Contact rhs) {
                if (lhs.equals(rhs)) {
                    return 0;
                }
                if (lhs.getTotalMoney() > rhs.getTotalMoney()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
    }

    private double countTotalMoney(String key) {
        List<Debt> debts = mDataChildren.get(key);
        if (debts == null || debts.size() == 0) {
            return 0;
        }
        double total = 0;
        for (Debt debt : debts) {
            total += debt.getMoneyAmount();
        }
        return total;
    }


//    @Override // TODO: 23/09/2015 separate menu
//    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
//        getActivity().getMenuInflater().inflate(R.menu.main, menu);
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        editModeMenuItem = menu.findItem(R.id.action_edit_mode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_mode:
                openEditView(selectedContact.getMapKey());
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setData(int leftIndex, int rightIndex) {
        double totalValue = 0;
        ArrayList<Entry> yVals = new ArrayList<>();

        // note: xIndex must be unique
        for (int i = leftIndex, xIndex = 0; i <= rightIndex; i++, xIndex++) {
            double amount = mDataHeaders.get(i).getTotalMoney();
            yVals.add(new Entry((float) amount, xIndex));// TODO: 22/09/2015 make sure it's money debt
            totalValue += amount;
        }

        ArrayList<String> xVals = new ArrayList<>();

        for (int i = leftIndex; i <= rightIndex; i++) {
            xVals.add(mDataHeaders.get(i).getName());
        }

        PieDataSet dataSet = new PieDataSet(yVals, getTag() + " debts");// TODO: 22/09/2015 by tag
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>(mColors);
        Collections.rotate(colors, -leftIndex);
        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(10f);

        if (mDataHeaders == null || rightIndex < 0) {
            mChart.setCenterText("Add money debts in list mode.");
            rangeBar.setVisibility(View.GONE);
        }
        else {
            mChart.setCenterText("Total Value\n" + totalValue + "\n(all slices)");
        }
        mChart.setData(data);

        // undo all highlights
        mChart.highlightValues(null);

        // refresh data
        mChart.invalidate();
    }

    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(String key) {
        List<Debt> debts = mDataChildren.get(key);
        if (debts == null || debts.size() == 0) {
            return; // just in case ;)
        }
        Debt debt;
        if (debts.size() == 1) {
            debt = debts.get(0);
        } else {
            debt = debts.get(0);// TODO: 10/5/2015 open detailed chart on select header
        }
        Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
    }
}
