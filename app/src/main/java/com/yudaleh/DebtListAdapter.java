package com.yudaleh;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.parse.ParseQueryAdapter;

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
    private List<Contact> mDataHeaders;
    private HashMap<String, Integer> mOwnerNamesCount;
    private HashMap<String, List<Debt>> mDataChildren;

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
        return this.mDataChildren.get(this.mDataHeaders.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.mDataHeaders.get(groupPosition);
    }

    private Debt getDebtFromPhone(String phone) {
        List<Debt> debts = mDataChildren.get(phone);
        if (debts == null || debts.size() == 0) {
            return null;
        }
        return debts.get(0);
    }

    private double getTotalMoney(String phone) {
        List<Debt> debts = mDataChildren.get(phone);
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
        return this.mDataChildren.get(this.mDataHeaders.get(groupPosition).getPhone()).get(childPosition);
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
        Contact owner = (Contact) getGroup(groupPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        String headerTitle = owner.getName();
        String phone = owner.getPhone();
        if (phone == null || headerTitle == null) {
            headerTitle = mContext.getString(R.string.no_phone_list_group);
        } else if (mOwnerNamesCount.get(headerTitle) > 1) {
            headerTitle += " (" + phone + ")";
        }
        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        if (owner.getTotalMoney() > 0) {
            convertView.setBackgroundColor(parent.getResources().getColor(mColors.get(groupPosition % mColors.size())));
        }
        return convertView;
    }

    public int convertDpToPixel(float dp) {
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
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
        SwipeListView childList = holder.childList;
        SettingsManager settings = SettingsManager.getInstance();// TODO: 04/10/2015 fix on best settings
        childList.setSwipeMode(settings.getSwipeMode());
        childList.setSwipeActionLeft(settings.getSwipeActionLeft());
        childList.setSwipeActionRight(settings.getSwipeActionRight());
        childList.setOffsetLeft(convertDpToPixel(settings.getSwipeOffsetLeft()));
        childList.setOffsetRight(convertDpToPixel(settings.getSwipeOffsetRight()));
        childList.setAnimationTime(settings.getSwipeAnimationTime());
        childList.setSwipeOpenOnLongPress(settings.isSwipeOpenOnLongPress());
        holder.childList.setAdapter(getAdapter(groupPosition, childPosition));

        return view;
    }


    private ArrayAdapter<Debt> getAdapter(int groupPosition, int childPosition) {
        return this.mChildrenAdapters.get(this.mDataHeaders.get(groupPosition)).get(childPosition);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
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
        super(context, queryFactory);
        mContext = context;

        mColors = new ArrayList<Integer>();
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

        // Init data to prevent NullPointerException
        mDataHeaders = new ArrayList<>();
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
                    mDataChildren = new HashMap<>();
                    mChildrenAdapters = new HashMap<>();
                    mOwnerNamesCount = new HashMap<>();
                    mDataHeaders = new ArrayList<>();

                    for (Debt debt : debts) {
                        String phone = debt.getPhone();
                        String name = debt.getOwner();
                        // TODO: 03/10/2015 update existing adapters
                        ArrayList<Debt> singleItemList = new ArrayList<>();
                        singleItemList.add(debt);
                        ArrayAdapter<Debt> swipeAdapter = new DebtSwipeListAdapter(mContext, R.layout.list_item, singleItemList);
                        if (!mDataChildren.containsKey(phone)) {
                            List<Debt> debtItems = new ArrayList<>();
                            debtItems.add(debt);
                            mDataChildren.put(phone, debtItems);

                            List<ArrayAdapter<Debt>> debtAdapters = new ArrayList<>();
                            debtAdapters.add(swipeAdapter);
                            mChildrenAdapters.put(phone, debtAdapters);

                            mDataHeaders.add(new Contact(phone, name, getTotalMoney(phone)));
                        } else {
                            mDataChildren.get(phone).add(debt);
                            mChildrenAdapters.get(phone).add(swipeAdapter);
                        }
                        if (!mOwnerNamesCount.containsKey(name)) {
                            mOwnerNamesCount.put(name, 1);
                        } else {
                            mOwnerNamesCount.put(name, mOwnerNamesCount.get(name) + 1);
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
                    notifyDataSetChanged(); // FIXME: 03/10/2015 index out of bounds when removed
                }
            }
        });
    }


    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(Debt debt) {// TODO: 04/10/2015 onclick
        Intent i = new Intent(mContext, EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        mContext.startActivity(i);
    }
}
