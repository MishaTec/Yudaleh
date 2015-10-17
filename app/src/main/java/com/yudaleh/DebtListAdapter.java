package com.yudaleh;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.parse.ParseQueryAdapter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


/**
 * Adapter between the list and the user's debts.
 */
class DebtListAdapter extends ParseQueryAdapter<Debt> implements /*PinnedSectionListAdapter,*/ ExpandableListAdapter {

    static final int ACTION_CALL = 0;
    static final int ACTION_SMS = 1;
    static final int ACTION_CHAT = 2;

    private final Context mContext;
    private final ArrayList<Integer> mColors;
    private HashMap<String, List<ArrayAdapter<Debt>>> mChildrenAdapters;
    private List<Header> mDataHeaders;
    private List<Debt> mSelectedData;
    HashMap<String, Integer> mOwnerNamesCount;
    private HashMap<String, List<Debt>> mDataChildren;
    private int mNonMoneyColor;

    // TODO: 29/09/2015 pinned heads
//    @Override public int getViewTypeCount() {
//        return 2;
//    }
//
//    @Override public int getItemViewType(int position) {
//        return getItem(position).getStatus();
//    }
//
//    @Override
//    public boolean isItemViewTypePinned(int viewType) {
//        return false;
//    }

    @Override
    public int getGroupCount() {
        return this.mDataHeaders.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.mDataChildren.get(this.mDataHeaders.get(groupPosition).getMapKey()).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.mDataHeaders.get(groupPosition);
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

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.mDataChildren.get(this.mDataHeaders.get(groupPosition).getMapKey()).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Header header = (Header) getGroup(groupPosition);
        String phone = header.getOwnerPhone();
        String name = header.getOwnerName();
        double totalMoneyAmount = header.getTotalMoney();
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        String totalMoneyStr = decimalFormat.format(totalMoneyAmount);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group, null);
        }

        String headerTitle = name;
        if (mOwnerNamesCount.get(name) > 1) {
            if (phone != null) {
                headerTitle += " (" + phone + ")";
            } // TODO: 09/10/2015 else, merge dialog (not here)
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);

        if (totalMoneyAmount > 0) {
            headerTitle += "\nTotal: " + totalMoneyStr + " NIS";
        }

        lblListHeader.setText(headerTitle);
        convertView.setBackgroundColor(header.getColor());

        return convertView;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View view, ViewGroup parent) {
        // FIXME: 03/10/2015 called too many times
        ViewHolder holder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_child, parent, false);
            holder = new ViewHolder();
            holder.childList = (SwipeListView) view.findViewById(R.id.child_list);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        final Debt debt = (Debt) getChild(groupPosition, childPosition);
        final SwipeListView swipeListView = holder.childList;
//        swipeListView.color(mColors.get(0));
        final View finalView = view;
        swipeListView.setSwipeListViewListener(new BaseSwipeListViewListener() {

            @Override
            public void onChoiceChanged(int position, boolean selected) {
                super.onChoiceChanged(position, selected);
                if (selected) {
                    swipeListView.setSelection(position);
                }
            }


            @Override
            public void onClickFrontView(int position) {
                openEditView(debt);
            }

/*            @Override
            public void onDismiss(int[] reverseSortedPositions) {
                for (int position : reverseSortedPositions) {
//                    System.out.println("dismiss: " + groupPosition + ", " + position);// REMOVE: 07/10/2015
                }
                swipeListView.getAdapter().notifyDataSetChanged();
            }*/

        });
        holder.childList.setAdapter(getAdapter(groupPosition, childPosition));
        return view;
    }

/*    private int getCountSelected() {
        return mSelectedData.size();
    }

    private void dismissSelected() {
        // TODO: 07/10/2015
    }*/


    private ArrayAdapter<Debt> getAdapter(int groupPosition, int childPosition) {
        return this.mChildrenAdapters.get(this.mDataHeaders.get(groupPosition).getMapKey()).get(childPosition);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {

    }

    @Override
    public void onGroupCollapsed(int groupPosition) {

    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return 0;
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        return 0;
    }

    private class ViewHolder {
        SwipeListView childList;
    }

    public void update() {
        loadObjects();
    }


    DebtListAdapter(Context context, QueryFactory<Debt> queryFactory) {
        super(context, queryFactory);// TODO: 10/5/2015 add order by _
        mContext = context;

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
        mNonMoneyColor = ColorTemplate.getHoloBlue();

        // Init data to prevent NullPointerException
        mDataHeaders = new ArrayList<>();
        mSelectedData = new ArrayList<>();
        mDataChildren = new HashMap<>();
        mChildrenAdapters = new HashMap<>();
        mOwnerNamesCount = new HashMap<>();

        addOnQueryLoadListener(new OnQueryLoadListener<Debt>() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(List<Debt> debts, Exception e) {
                if (e == null) {
                    extractData(debts);
                    notifyDataSetChanged(); // FIXME: 03/10/2015 index out of bounds when removed
                }
            }
        });
    }

    private void extractData(List<Debt> debts) {
        mDataChildren = new HashMap<>();
        mChildrenAdapters = new HashMap<>();
        mOwnerNamesCount = new HashMap<>();
        mDataHeaders = new ArrayList<>();
        mSelectedData = new ArrayList<>();

        for (Debt debt : debts) {
            String phone = debt.getOwnerPhone();
            String name = debt.getOwnerName();
            Header header = new Header(phone, name);
            String key = header.getMapKey();

            // TODO: 03/10/2015 update existing adapters
            ArrayList<Debt> singleItemList = new ArrayList<>();
            singleItemList.add(debt);
            ArrayAdapter<Debt> swipeAdapter = new DebtSwipeListAdapter(mContext, R.layout.list_item, singleItemList);
            if (!mDataChildren.containsKey(key)) {
                List<Debt> debtItems = new ArrayList<>();
                debtItems.add(debt);
                mDataChildren.put(key, debtItems);

                List<ArrayAdapter<Debt>> debtAdapters = new ArrayList<>();
                debtAdapters.add(swipeAdapter);
                mChildrenAdapters.put(key, debtAdapters);

                mDataHeaders.add(header);
                if (!mOwnerNamesCount.containsKey(name)) {
                    mOwnerNamesCount.put(name, 1);
                } else {
                    mOwnerNamesCount.put(name, mOwnerNamesCount.get(name) + 1);
                }
            } else {

                mDataChildren.get(key).add(debt);
                mChildrenAdapters.get(key).add(swipeAdapter);
            }

            for (Header c : mDataHeaders) {
                c.setTotalMoney(countTotalMoney(c.getMapKey()));
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
        int i = 0;
        int numColors = mColors.size();
        for (Header header : mDataHeaders) {
            header.setColor(header.getTotalMoney() > 0 ? mColors.get(i++ % numColors) : mNonMoneyColor);
        }
    }


    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(Debt debt) {// TODO: 04/10/2015 onclick
        Intent i = new Intent(mContext, EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        mContext.startActivity(i);
    }
}
