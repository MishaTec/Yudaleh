package com.yudaleh;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import com.melnykov.fab.FloatingActionButton;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class ListViewFragment extends android.support.v4.app.Fragment {

    // Adapter for the Debts Parse Query
    DebtListAdapter debtListAdapter;
    private LinearLayout noDebtsView;
    private ExpandableListView listView;
    private View mRoot;
    ParseQueryAdapter.QueryFactory<Debt> factory;
    private FloatingActionButton fab;

    public ListViewFragment() {
        // Set up the Parse mQuery to use in the adapter
        factory = new ParseQueryAdapter.QueryFactory<Debt>() {
            public ParseQuery<Debt> create() {
                ParseQuery<Debt> query = Debt.getQuery();
                query.whereEqualTo(Debt.KEY_TAB_TAG, getTag());
                query.orderByDescending(Debt.KEY_DATE_CREATED);
                query.fromPin(DebtListApplication.DEBT_GROUP_NAME);
                return query;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }

        View root = inflater.inflate(R.layout.fragment_listview, container, false);
        // Set up the views
//        listView = (SwipeListView) root.findViewById(R.id.debts_list);// TODO: 30/09/2015 merge
        listView = (ExpandableListView) root.findViewById(R.id.debts_list);
        noDebtsView = (LinearLayout) root.findViewById(R.id.no_debts_view);
        listView.setEmptyView(noDebtsView);

        // Set up the adapter
        debtListAdapter = new DebtListAdapter(getActivity(), factory);

        // Attach the mQuery adapter to the view
        listView.setAdapter((ExpandableListAdapter) debtListAdapter);

        fab = (FloatingActionButton) root.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
                i.putExtra(Debt.KEY_TAB_TAG, getTag());
                startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
            }
        });
        fab.attachToListView(listView, null);

        mRoot = root;
        return root;
    }

    @Override
    public void onResume() {
        updateView();
        super.onResume();
    }

    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(Debt debt) {
        Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
    }

    void updateView() {
        if(debtListAdapter!=null) {
            debtListAdapter.update();
        }
        fab.show(false);
    }

    void clearView() {
        debtListAdapter.clear();
    }
}
