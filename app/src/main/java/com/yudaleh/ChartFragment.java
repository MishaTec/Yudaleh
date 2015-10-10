package com.yudaleh;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;
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

    private static final int MODE_HEADERS = 0;
    private static final int MODE_DEBTS = 1;

    private static final long ANIMATION_DURATION = 100;
    private static final float ANIMATION_OFFSET = 0.5f;
    private static final int ANIMATION_PIVOT_TYPE = Animation.RELATIVE_TO_SELF;
    private static final float SHRINK_FROM = 1.0f;
    private static final float SHRINK_TO = 0.1f;
    private static final float GROW_FROM = SHRINK_TO;
    private static final float GROW_TO = SHRINK_FROM;

    private final View.OnClickListener addListener;
    private final View.OnClickListener expandMoreListener;
    private final View.OnClickListener expandLessListener;
    private final View.OnClickListener editListener;
    private final ScaleAnimation growAnim;

    private int displayMode = MODE_HEADERS;// TODO: 06/10/2015 set to default on refresh

    private int selectedIndex;
    private Object selectedObject;

    private View mRoot;
    private PieChart mChart;
    private RangeBar rangeBar;
    private TextView maxIndexView;

    private TextView minIndexView;
    private final ArrayList<Integer> mColors;
    private List<Header> mDataHeaders;
    private List mCurrentDataHeaders;
    private HashMap<String, Integer> mOwnerNamesCount;
    private HashMap<String, Integer> mOwnerNamesCountNoPhone;
    private HashMap<String, List<Debt>> mDataChildren;
    private int mMaxDebtIndex;
    private FloatingActionButton fab;

    public ChartFragment() {
        growAnim = new ScaleAnimation(GROW_FROM, GROW_TO, GROW_FROM, GROW_TO, ANIMATION_PIVOT_TYPE, ANIMATION_OFFSET, ANIMATION_PIVOT_TYPE, ANIMATION_OFFSET);
        growAnim.setDuration(ANIMATION_DURATION);

        addListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditView(null);
            }
        };
        expandMoreListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentDataHeaders = mDataChildren.get(getSelectedKey());
                setDataFromHeaders();
                displayMode = MODE_DEBTS;
                switchFabMode(R.drawable.ic_expand_less_white_24dp, R.color.accent_expand_less, R.color.accent_pressed_expand_less, R.color.ripple_expand_less, expandLessListener);
            }
        };
        expandLessListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentDataHeaders = mDataHeaders;
                setDataFromHeaders();
                displayMode = MODE_HEADERS;
                switchFabMode(R.drawable.ic_add_white_24dp, R.color.accent_add, R.color.accent_pressed_add, R.color.ripple_add, addListener);
            }
        };
        editListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditView(getSelectedKey());
            }
        };

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

    private String getSelectedKey() {
        if (selectedObject instanceof Header) {
            Header selectedHeader = (Header) selectedObject;
            return selectedHeader.getMapKey();
        } else if (selectedObject instanceof Debt) {
            Debt selectedDebt = (Debt) selectedObject;
            String ownerPhone = selectedDebt.getOwnerPhone();
            return ownerPhone != null ? ownerPhone : selectedDebt.getOwnerName();
        }
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }

        View v = inflater.inflate(R.layout.fragment_chart, container, false);

        mChart = (PieChart) v.findViewById(R.id.pie_chart);
        mChart.setDescription("");

//        mChart.setUsePercentValues(false);// REMOVE: 06/10/2015

        mChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartLongPressed(MotionEvent me) {
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
                if (displayMode == MODE_HEADERS) {
                    switchFabMode(R.drawable.ic_add_white_24dp, R.color.accent_add, R.color.accent_pressed_add, R.color.ripple_add, addListener);
                } else {
                    switchFabMode(R.drawable.ic_expand_less_white_24dp, R.color.accent_expand_less, R.color.accent_pressed_expand_less, R.color.ripple_expand_less, expandLessListener);
                }
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
                selectedIndex = e.getXIndex();
                selectedObject = mCurrentDataHeaders.get(selectedIndex);
                if (displayMode == MODE_DEBTS) {
                    switchFabMode(R.drawable.ic_mode_edit_white_24dp, R.color.accent_edit, R.color.accent_pressed_edit, R.color.ripple_edit, editListener);
                    return;
                } // Otherwise, it depends on the number of debts in the section
                List<Debt> debts = mDataChildren.get(getSelectedKey());
                if (debts != null && debts.size() > 1) {
                    switchFabMode(R.drawable.ic_expand_more_white_24dp, R.color.accent_expand_more, R.color.accent_pressed_expand_more, R.color.ripple_expand_more, expandMoreListener);
                } else {
                    selectedIndex = 0; // only the first (and only) debt is accessed
                    switchFabMode(R.drawable.ic_mode_edit_white_24dp, R.color.accent_edit, R.color.accent_pressed_edit, R.color.ripple_edit, editListener);
                }
            }

            @Override
            public void onNothingSelected() {
                if (displayMode == MODE_HEADERS) {
                    switchFabMode(R.drawable.ic_add_white_24dp, R.color.accent_add, R.color.accent_pressed_add, R.color.ripple_add, addListener);
                } else {
                    switchFabMode(R.drawable.ic_expand_less_white_24dp, R.color.accent_expand_less, R.color.accent_pressed_expand_less, R.color.ripple_expand_less, expandLessListener);
                }
            }
        });

        fab = (FloatingActionButton) v.findViewById(R.id.fab2);
        fab.setOnClickListener(addListener);

        maxIndexView = (TextView) v.findViewById(R.id.max_debt_index);
        minIndexView = (TextView) v.findViewById(R.id.min_debt_index);

        rangeBar = (RangeBar) v.findViewById(R.id.rangeBar);

        rangeBar.setBarColor(getResources().getColor(R.color.primary_dark));
        rangeBar.setConnectingLineColor(getResources().getColor(R.color.primary));

        rangeBar.setThumbColorNormal(getResources().getColor(R.color.accent_add));
        rangeBar.setThumbColorPressed(getResources().getColor(R.color.accent_pressed_add));

        rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
                if (rightThumbIndex > mMaxDebtIndex) {
                    rightThumbIndex = mMaxDebtIndex;
                }
                minIndexView.setText(String.valueOf(leftThumbIndex + 1));
                maxIndexView.setText(String.valueOf(rightThumbIndex + 1));
                setData(leftThumbIndex, rightThumbIndex);
            }
        });

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);

        mRoot = v;
        return v;
    }

    private void switchFabMode(final int iconResId, final int colorNormalResId, final int colorPressedResId, final int colorRippleResId, final View.OnClickListener listener) {
        final ScaleAnimation shrinkAnim = new ScaleAnimation(SHRINK_FROM, SHRINK_TO, SHRINK_FROM, SHRINK_TO, ANIMATION_PIVOT_TYPE, ANIMATION_OFFSET, ANIMATION_PIVOT_TYPE, ANIMATION_OFFSET);
        shrinkAnim.setDuration(ANIMATION_DURATION);
        shrinkAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab.setImageResource(iconResId);
                fab.setColorNormalResId(colorNormalResId);
                fab.setColorPressedResId(colorPressedResId);
                fab.setColorRippleResId(colorRippleResId);
                fab.setOnClickListener(listener);
                fab.refreshDrawableState();

                fab.startAnimation(growAnim);
            }
        });

        fab.startAnimation(shrinkAnim);
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
            mCurrentDataHeaders = mDataHeaders;
        }
        switchFabMode(R.drawable.ic_add_white_24dp, R.color.accent_add, R.color.accent_pressed_add, R.color.ripple_add, addListener);
        displayMode = MODE_HEADERS;// TODO: 06/10/2015 return to last seen view (f)
        setDataFromHeaders();
    }

    private void setDataFromHeaders() {
        if (mCurrentDataHeaders != null) {
            mMaxDebtIndex = mCurrentDataHeaders.size() - 1;
        } else {
            mMaxDebtIndex = -1;
        }
        int tickCount = Math.max(mMaxDebtIndex + 1, RANGE_BAR_MIN_TICK_COUNT);
        rangeBar.setTickCount(tickCount);
        rangeBar.setThumbIndices(0, tickCount - 1);
        int rightThumbIndex = rangeBar.getRightIndex();
        if (rightThumbIndex > mMaxDebtIndex) {
            rightThumbIndex = mMaxDebtIndex;
        }
        int leftThumbIndex = rangeBar.getLeftIndex();
        minIndexView.setText(String.valueOf(leftThumbIndex + 1));
        maxIndexView.setText(String.valueOf(rightThumbIndex + 1));
        setData(leftThumbIndex, rightThumbIndex);
    }

    // TODO: 10/5/2015 use only once, no need to accesses database again
    private void extractData(List<Debt> debts) {
        mDataChildren = new HashMap<>();
        mOwnerNamesCount = new HashMap<>();
        mOwnerNamesCountNoPhone = new HashMap<>();
        mDataHeaders = new ArrayList<>();

        for (Debt debt : debts) {
            String phone = debt.getOwnerPhone();
            // TODO: 05/10/2015 dialog for merging non-phone debts
            String name = debt.getOwnerName();
            Header header = new Header(phone, name);
            String key = header.getMapKey();

            // TODO: 03/10/2015 update existing adapters
            if (!mDataChildren.containsKey(key)) {
                List<Debt> debtItems = new ArrayList<>();
                debtItems.add(debt);
                mDataChildren.put(key, debtItems);

                mDataHeaders.add(header);
            } else {
                mDataChildren.get(key).add(debt);
            }

            for (Header c : mDataHeaders) {
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
        Collections.sort(mDataHeaders, new Comparator<Header>() {
            @Override
            public int compare(Header lhs, Header rhs) {
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
        mCurrentDataHeaders = mDataHeaders;
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

    private void setData(int leftIndex, int rightIndex) {
        if (mCurrentDataHeaders == null) {
            return;
        }

        double totalValue = 0;
        ArrayList<Entry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();
        String label = "Debts";

        // note: xIndex must be unique
        for (int i = leftIndex, xIndex = 0; i <= rightIndex; i++, xIndex++) {
            Object currItem = mCurrentDataHeaders.get(i);
            double amount = 0;
            if (currItem instanceof Header) {
                Header currHeader = (Header) currItem;
                amount = currHeader.getTotalMoney();
                xVals.add(currHeader.getOwnerName());
                yVals.add(new Entry((float) amount, xIndex));// TODO: 22/09/2015 make sure it's money debt
                label = mMaxDebtIndex + " debts";
            } else if (currItem instanceof Debt) {
                Debt currDebt = (Debt) currItem;
                amount = currDebt.getMoneyAmount();
                xVals.add(currDebt.getTitle());
                yVals.add(new Entry((float) amount, xIndex));// TODO: 22/09/2015 make sure it's money debt
                label = currDebt.getOwnerName();
            }
            totalValue += amount;
        }

        PieDataSet dataSet = new PieDataSet(yVals, label);// TODO: 22/09/2015 by tag
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(7f);

        ArrayList<Integer> colors = new ArrayList<>(mColors);
        Collections.rotate(colors, -leftIndex);
        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {

                if (displayMode == MODE_DEBTS) {
                    Debt debt = (Debt) mCurrentDataHeaders.get(entry.getXIndex());
                    String currency = debt.getTitle().split(" ")[1];// TODO: 06/10/2015 make sure no exception
                    int currencyPos = debt.getCurrencyPos();
//                    if(currencyPos == )
                    return String.valueOf(value);
                } else {
                    return String.valueOf(value);
                }
            }
        });
        data.setValueTextSize(10f);

        if (mCurrentDataHeaders == null || rightIndex < 0) {
            mChart.setCenterText("Add money debts in list mode.");
            rangeBar.setVisibility(View.INVISIBLE);
            minIndexView.setVisibility(View.INVISIBLE);
            maxIndexView.setVisibility(View.INVISIBLE);
        } else {
            mChart.setCenterText("Total Value\n" + totalValue + "\n(all slices)");
            rangeBar.setVisibility(View.VISIBLE);
            minIndexView.setVisibility(View.VISIBLE);
            maxIndexView.setVisibility(View.VISIBLE);
        }
        mChart.setData(data);

        // undo all highlights
        mChart.highlightValues(null);

        // refresh data
        mChart.invalidate();
    }

    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(String key) {
        if (key == null) {
            Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
            i.putExtra(Debt.KEY_TAB_TAG, getTag());
            startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
        }
        List<Debt> debts = mDataChildren.get(key);
        if (debts == null || debts.size() <= selectedIndex) {
            return; // just in case ;)
        }
        Debt debt = debts.get(selectedIndex);
        Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
    }
}
