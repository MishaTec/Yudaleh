package com.yudaleh;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int LOGIN_ACTIVITY_CODE = 100;
    static final int SETTINGS_ACTIVITY_CODE = 100;
    static final int EDIT_ACTIVITY_CODE = 200;
    static final int EDIT_ACTIVITY_FRAGMENT_CODE = 65736;
    static final String USER_CHANNEL_PREFIX = "t";

    static final String FULLSCREEN_FRAGMENT_TAG = "photoZoom";

    private static final int I_OWE_TAB_INDEX = 0;
    private static final int OWE_ME_TAB_INDEX = 1;

    static final String ALARM_SCHEME = "timer:";

    private static final boolean SHOW_LOGIN_ON_ERROR = true;
    static final String PARSE_USER_NAME_KEY = "name";
    static final String PARSE_USER_PHONE_KEY = "phone";

    private boolean mIsShowLoginOnFail = false;
    private boolean mWasSignupShowen = false;

    private int numPinned;//// TODO: 05/09/2015 remove
    private int numSaved;//// TODO: 05/09/2015 remove

    private BroadcastReceiver receiver = null;

    ListViewFragment iOweViewFragment;
    ListViewFragment oweMeViewFragment;
    ChartFragment oweMeChartFragment;
    ChartFragment iOweChartFragment;

    MenuItem syncMenuItem;
    MenuItem loginMenuItem;
    MenuItem logoutMenuItem;
    MenuItem chartModeMenuItem;
    MenuItem listModeMenuItem;

    private boolean isChartMode;

    private Intent serviceIntent;
    private boolean isSyncOptionVisible = true;
    private Toast backToast;

//    ListViewFragment iOweViewFragmentWithTag;// REMOVE: 29/09/2015
//    ListViewFragmentOweMe oweMeViewFragmentWithTag;


    // TODO: 26/09/2015 make messaging, call and push options only for logged in
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serviceIntent = new Intent(getApplicationContext(), MessageService.class);
        ParseUser currUser = ParseUser.getCurrentUser();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Boolean success = intent.getBooleanExtra("success", false);
                if (!success) {
                    Toast.makeText(getApplicationContext(), "Messaging service failed to start", Toast.LENGTH_LONG).show();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("com.yudaleh.MainActivity"));

        if (currUser != null) {
            startService(serviceIntent);
        }

        initActionBar();

/*        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ on create");
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Debt.KEY_UUID)) {
            String uuid = intent.getStringExtra(Debt.KEY_UUID);
            openEditView(uuid);
        }*/// REMOVE: 08/09/2015
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (iOweViewFragmentWithTag == null) {
//            iOweViewFragmentWithTag = (ListViewFragment) getSupportFragmentManager().findFragmentByTag(Debt.I_OWE_TAG);
//        }
//        if (oweMeViewFragmentWithTag == null) {
//            oweMeViewFragmentWithTag = (ListViewFragmentOweMe) getSupportFragmentManager().findFragmentByTag(Debt.OWE_ME_TAG);
//        }
        // Check if we have a logged in user
        ParseUser currUser = ParseUser.getCurrentUser();

        if (currUser != null) {
            // Sync data to Parse
            syncDebtsToParse(!SHOW_LOGIN_ON_ERROR);// TODO: 19/09/2015 make sure it's called after on result from login, so no accidental debts are uploaded
            // Update the title
            updateLoggedInInfo();
        } else {
            // In case the user is logged out, the sync option works as login
            setSyncOptionVisibility(true);
        }
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("isResponsePush")) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.selectTab(actionBar.getTabAt(I_OWE_TAB_INDEX));
        } else if (intent != null && intent.hasExtra(Debt.KEY_TAB_TAG)) {
            String tabTag = intent.getStringExtra(Debt.KEY_TAB_TAG);
            ActionBar actionBar = getSupportActionBar();
            if (tabTag.equals(Debt.I_OWE_TAG)) {
                actionBar.selectTab(actionBar.getTabAt(I_OWE_TAB_INDEX));
            } else {
                actionBar.selectTab(actionBar.getTabAt(OWE_ME_TAB_INDEX));
            }
        }
        updateLoggedInInfo();
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, MessageService.class));
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager != null ? fragmentManager.findFragmentByTag(FULLSCREEN_FRAGMENT_TAG) : null;
        if (currentFragment != null) {
            ((FullscreenFragment) currentFragment).shrinkBack();
        } else if (isChartMode) {
            switchDisplayMode();
        } else if (backToast != null && backToast.getView().getWindowToken() != null) {
            super.onBackPressed();
            finish();
        } else {
            backToast = Toast.makeText(this, " Press Back again to Exit ", Toast.LENGTH_SHORT);
            backToast.show();
        }
    }
/*    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra(Debt.KEY_UUID)) {
            String uuid = intent.getStringExtra(Debt.KEY_UUID);
            intent.removeExtra(Debt.KEY_UUID);
            openEditView(uuid);
        }
    }*/// REMOVE: 08/09/2015


    @SuppressWarnings("deprecation")
    private void initActionBar() {
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab()
                    .setText(getString(R.string.i_owe_tab_title))
                    .setTabListener(new ActionBar.TabListener() {
                        @Override
                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            if (isChartMode) {
                                if (iOweChartFragment == null) {
                                    iOweChartFragment = new ChartFragment();
                                }
                                fragmentTransaction.replace(android.R.id.content, iOweChartFragment, Debt.I_OWE_TAG);
                            } else {
                                if (iOweViewFragment == null) {
                                    iOweViewFragment = new ListViewFragment();// TODO: 9/18/2015 update on login
                                }
                                fragmentTransaction.replace(android.R.id.content, iOweViewFragment, Debt.I_OWE_TAG);
                            }
                        }

                        @Override
                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }

                        @Override
                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            onTabSelected(tab, fragmentTransaction);
                        }
                    }));
            actionBar.addTab(actionBar.newTab()
                    .setText(getString(R.string.owe_me_tab_title))
                    .setTabListener(new ActionBar.TabListener() {
                        @Override
                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                            if (oweMeViewFragment == null || oweMeViewFragmentWithTag == null) {
//                                oweMeViewFragment = new ListViewFragmentOweMe();
//                                fragmentTransaction.replace(android.R.id.content, oweMeViewFragment, Debt.OWE_ME_TAG);
//                                oweMeViewFragmentWithTag = (ListViewFragmentOweMe) getSupportFragmentManager().findFragmentByTag(Debt.OWE_ME_TAG);
//                            } else {// REMOVE: 23/09/2015
                            if (isChartMode) {
                                if (oweMeChartFragment == null) {
                                    oweMeChartFragment = new ChartFragment();
                                }
                                fragmentTransaction.replace(android.R.id.content, oweMeChartFragment, Debt.OWE_ME_TAG);
                            } else {
                                if (oweMeViewFragment == null) {
                                    oweMeViewFragment = new ListViewFragment();
                                }
                                fragmentTransaction.replace(android.R.id.content, oweMeViewFragment, Debt.OWE_ME_TAG);
                            }
//                            }
                        }

                        @Override
                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }

                        @Override
                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            onTabSelected(tab, fragmentTransaction);
                        }
                    }));
//            actionBar.addTab(actionBar.newTab()
//                    .setText(getString(R.string.dashboard_tab_title))
//                    .setTabListener(new ActionBar.TabListener() {
//                        @Override
//                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                            if (iOweChartFragment == null) {
//                                iOweChartFragment = new ChartFragment();
//                            }
//                            fragmentTransaction.replace(android.R.id.content, iOweChartFragment, DASHBOARD_TAB_TAG);
//                        }
//
//                        @Override
//                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                        }
//
//                        @Override
//                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                        }
//                    }));// REMOVE: 22/09/2015
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        syncMenuItem = menu.findItem(R.id.action_sync);
        loginMenuItem = menu.findItem(R.id.action_login);
        logoutMenuItem = menu.findItem(R.id.action_logout);
        chartModeMenuItem = menu.findItem(R.id.action_chart_mode);
        listModeMenuItem = menu.findItem(R.id.action_list_mode);

        updateMenuItemsVisibility();

        return super.onCreateOptionsMenu(menu);
    }


    private void updateMenuItemsVisibility() {
        ParseUser currUser = ParseUser.getCurrentUser();

        boolean isRealUser = currUser != null;
        loginMenuItem.setVisible(!isRealUser);
        logoutMenuItem.setVisible(isRealUser);

        chartModeMenuItem.setVisible(!isChartMode);
        listModeMenuItem.setVisible(isChartMode);
        syncMenuItem.setVisible(isSyncOptionVisible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_ACTIVITY_CODE);
                break;

            case R.id.action_sync:
                syncDebtsToParse(SHOW_LOGIN_ON_ERROR);
                break;

            case R.id.action_chart_mode:
            case R.id.action_list_mode:
                switchDisplayMode();
                break;

            case R.id.action_logout:
                logoutFromParse();
                break;

            case R.id.action_login:
                openLoginView();
                break;
            case R.id.about:
                TextView content = (TextView) getLayoutInflater().inflate(R.layout.about_view, null);
                content.setMovementMethod(LinkMovementMethod.getInstance());
                countSavedAndPinnedObjects();
                content.setText(Html.fromHtml(getString(R.string.about_body)) + "\npinned: " + numPinned + "\nsaved: " + numSaved);// TODO: 11/10/2015 clean
                // REMOVE: 07/09/2015 info
/*                ParseUser curr = ParseUser.getCurrentUser();
                if (curr != null) {
                    String token = curr.getSessionToken();
                    boolean isAuth = curr.isAuthenticated();
                    boolean isDataAvai = curr.isDataAvailable();
                    boolean isNew = curr.isNew();
                    boolean isDirty = curr.isDirty();
                    boolean isDirtyFixed = false;
                    boolean isLinked = ParseAnonymousUtils.isLinked(curr);
                    countSavedAndPinnedObjects();
                    String dirtyKey = null;
                    String keys = Arrays.toString(curr.keySet().toArray());
                    int numDirty = 0;
                    if (isDirty) {
                        for (String key : curr.keySet()) {
                            if (curr.isDirty(key)) {
                                numDirty++;
                                dirtyKey = key;
                            }
                        }
                        // TODO: 05/09/2015 fix dirty
                        curr = ParseUser.getCurrentUser();
                        isDirty = curr.isDirty();
                        if (!isDirty) {
                            isDirtyFixed = true;
                        }
                    }
                    String info = "\nphone : " + curr.getString(PARSE_USER_PHONE_KEY) + "\nuser: " + curr.getUsername() + "\nisAuth: " + isAuth + "\nisDataAvai: " + isDataAvai + "\nisNew: " + isNew + "\nisDirty: " + isDirty + (isDirtyFixed ? " (fixed)" : "") + "\nkeys: " + keys + "\ndirtyKey: " + dirtyKey + "\nnumDirty: " + numDirty + "\ntoken: " + token + "\nisLinked: " + isLinked + "\npinned: " + numPinned + "\nsaved: " + numSaved;
                    content.setText(info);// REMOVE: 14/09/2015
                }*/
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about)
                        .setView(content)
                        .setInverseBackgroundForced(true)// FIXME: 06/09/2015
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void switchDisplayMode() {
        isChartMode = !isChartMode;
        chartModeMenuItem.setVisible(!isChartMode);
        listModeMenuItem.setVisible(isChartMode);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && actionBar.getSelectedTab() != null) {
            actionBar.getSelectedTab().select();
        }
    }

    private void countSavedAndPinnedObjects() {
        // Count pinned:
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromPin(DebtListApplication.DEBT_GROUP_NAME);
        List<Debt> debts;
        try {
            debts = query.find();
            numPinned = debts.size();
        } catch (ParseException e) {
            numPinned = -1;
        }

        // Count saved:
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser == null) {
            numSaved = -2;
            return;
        }
        query = Debt.getQuery();
        query.whereEqualTo(Debt.KEY_AUTHOR_PHONE, currUser.getString(PARSE_USER_PHONE_KEY));
        try {
            debts = query.find();
            numSaved = debts.size();
        } catch (ParseException e) {
            numSaved = -1;
        }
    }

    private void cancelAllAlarmsOnPinnedObjects() {
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromPin(DebtListApplication.DEBT_GROUP_NAME); // FIXME: 11/10/2015 local datastore
        query.findInBackground(new FindCallback<Debt>() {
            public void done(List<Debt> debts, ParseException e) {
                if (debts != null) {
                    for (final Debt debt : debts) {
                        Date dueDate = debt.getDueDate();
                        if (dueDate != null) {
                            cancelAlarm(debt);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // An OK result means the pinned dataset changed or
        // log in was successful
        if (resultCode == RESULT_OK) {
            if (requestCode == EDIT_ACTIVITY_CODE || requestCode == EDIT_ACTIVITY_FRAGMENT_CODE) {
                // Coming back from the edit view, update the view
                // REMOVE: 07/09/2015 debtListAdapter.loadObjects();
                if (data != null && data.hasExtra(Debt.KEY_TAB_TAG)) {
//                    String tabTag = data.getStringExtra(Debt.KEY_TAB_TAG);
//                    if (tabTag.equals(Debt.I_OWE_TAG) && iOweViewFragmentWithTag != null) {
//                        iOweViewFragment.updateView();
//                    } else if (oweMeViewFragmentWithTag != null) {
//                        oweMeViewFragmentWithTag.updateView();
//                    }
                }
            } else if (requestCode == LOGIN_ACTIVITY_CODE) {
                // If the user is new, sync data to Parse, otherwise get the current list from Parse
                ParseUser currUser = ParseUser.getCurrentUser();
                if (currUser != null) {
                    startService(serviceIntent);
                    subscribeToPush();// TODO: 24/09/2015 settings
                    syncDebtsToParse(SHOW_LOGIN_ON_ERROR);// FIXME: 06/09/2015 add if
                    if (!currUser.isNew()) {
                        loadFromParse();
                    }
                }
                updateLoggedInInfo();// TODO: 05/09/2015 remove?
                ActionBar actionBar = getSupportActionBar();
                actionBar.selectTab(actionBar.getSelectedTab());// FIXME: 08/10/2015 make sure it refreshes
            }
        }

    }

    private void subscribeToPush() {
        List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser == null) {
            return;
        }
        String phone = currUser.getString(PARSE_USER_PHONE_KEY);
        if (phone == null) {
            return;
        }
        String currUserChannel = USER_CHANNEL_PREFIX + phone.replaceAll("[^0-9]+", "");
        if (subscribedChannels == null || !subscribedChannels.contains(currUserChannel)) {
            ParsePush.subscribeInBackground(currUserChannel);
        }
    }

    private void unsubscribeFromPush() {
        List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser == null) {
            return;
        }
        String phone = currUser.getString(PARSE_USER_PHONE_KEY);
        if (phone == null) {
            return;
        }
        String currUserChannel = USER_CHANNEL_PREFIX + phone.replaceAll("[^0-9]+", "");
        if (subscribedChannels != null && subscribedChannels.contains(currUserChannel)) {
            ParsePush.unsubscribeInBackground(currUserChannel);
        }
    }

    private void openLoginView() {
        ParseLoginBuilder builder = new ParseLoginBuilder(getApplicationContext());
        startActivityForResult(builder.build(), LOGIN_ACTIVITY_CODE);
    }

    private void logoutFromParse() {
        stopService(new Intent(this, MessageService.class));
        unsubscribeFromPush();
        // Log out the current user
        ParseUser.logOut();
        // Create a new anonymous user
//        ParseAnonymousUtils.logIn(null);// FIXME: 02/09/2015
        // Clear the view
//        if (iOweViewFragmentWithTag != null) {
        iOweViewFragment.clearView();// FIXME: 17/09/2015
//        }
//        if (oweMeViewFragmentWithTag != null) {
//            oweMeViewFragmentWithTag.clearView();
//        }
        cancelAllAlarmsOnPinnedObjects();// TODO: 09/09/2015 not only pinned ?
        // Unpin all the current objects
        ParseObject.unpinAllInBackground(DebtListApplication.DEBT_GROUP_NAME, new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {// REMOVE: 08/10/2015
                    Toast.makeText(MainActivity.this, "Failed to unpin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        // Update the logged in label info
        updateLoggedInInfo();

        // Refresh tab content
        ActionBar actionBar = getSupportActionBar();
        actionBar.selectTab(actionBar.getSelectedTab());// FIXME: 08/10/2015 make sure it refreshes

        // Makes sure the menu is consistent
        supportInvalidateOptionsMenu();
    }


    private void syncDebtsToParse(final boolean isShowLoginOnFail) {
        // We could use saveEventually here, but we want to have some UI
        // around whether or not the draft has been saved to Parse
        mWasSignupShowen = false;// FIXME: 06/09/2015 ?
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if ((ni != null) && (ni.isConnected())) {
            final ParseUser currUser = ParseUser.getCurrentUser();
            if (currUser != null) {
                // If we have a network connection and a current logged in user, sync the debts
                // In this app, local changes should overwrite content on the server.
                ParseQuery<Debt> query = Debt.getQuery();
                query.fromPin(DebtListApplication.DEBT_GROUP_NAME);
                query.whereEqualTo("isDraft", true);
                query.findInBackground(new FindCallback<Debt>() {
                    public void done(List<Debt> debts, ParseException e) {
                        if (e == null) {
                            setSyncOptionVisibility(false);
                            for (final Debt debt : debts) {
                                // Set is draft flag to false before
                                // syncing to Parse
                                debt.setDraft(false);
                                debt.setAuthorName(currUser.getString(PARSE_USER_NAME_KEY));
                                debt.setAuthorPhone(currUser.getString(PARSE_USER_PHONE_KEY));
                                debt.saveInBackground(new SaveCallback() {

                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            // Let adapter know to update view
                                            if (!isFinishing()) {
                                                // REMOVE: 07/09/2015 debtListAdapter.notifyDataSetChanged();
//                                                if (debt.getTabTag().equals(Debt.I_OWE_TAG) && iOweViewFragmentWithTag != null) {
//                                                    iOweViewFragment.updateView();
//                                                } else if (oweMeViewFragmentWithTag != null) {
//                                                    oweMeViewFragmentWithTag.updateView();
//                                                }
                                            }
                                        } else {
                                            if (!isShowLoginOnFail) {
/*                                                Toast.makeText(getApplicationContext(),
                                                        e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();*/// REMOVE: 09/09/2015
                                            }
                                            // Reset the is draft flag locally to true
                                            debt.setDraft(true);
                                            setSyncOptionVisibility(true);
                                            // Save flag field as late as possible - to deal with
                                            // asynchronous callback
                                            mIsShowLoginOnFail = isShowLoginOnFail;
                                            handleParseError(e);// FIXME: 05/09/2015
                                        }
                                    }

                                });
                            }
                        } else {
                            setSyncOptionVisibility(true);
                            Log.i("DebtListActivity",
                                    "syncDebtsToParse: Error finding pinned debts: "
                                            + e.getMessage());
                        }
                    }
                });
            } else if (isShowLoginOnFail) {
                // If we have a network connection but no logged in user, direct
                // the person to log in or sign up.
                openLoginView();
            }
        } else {
            // If there is no connection, let the user know the sync didn't
            // happen
            Toast.makeText(// TODO: 26/09/2015 test case
                    getApplicationContext(),
                    "Your device appears to be offline. Some debts may not have been synced to Parse.",
                    Toast.LENGTH_LONG).show();
        }

    }

    private void setSyncOptionVisibility(boolean newValue) {
        boolean updateMenu = false;
        if (isSyncOptionVisible != newValue) {
            // In case the status was changed, make sure the icon visibility is updated
            updateMenu = true;
        }
        isSyncOptionVisible = newValue;
        if (updateMenu) {
            supportInvalidateOptionsMenu();
        }
    }

    private void loadFromParse() {
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser == null) {
            return;
        }
        ParseQuery<Debt> query = Debt.getQuery();
        query.whereEqualTo(Debt.KEY_AUTHOR_PHONE, currUser.getString(PARSE_USER_PHONE_KEY));
        query.findInBackground(new FindCallback<Debt>() {
            public void done(final List<Debt> debts, ParseException e) {
                if (e == null) {
                    ParseObject.pinAllInBackground(DebtListApplication.DEBT_GROUP_NAME, debts,
                            new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        if (!isFinishing()) {
                                            for (final Debt debt : debts) {
                                                Date dueDate = debt.getDueDate();
                                                if (dueDate != null) {
                                                    setAlarm(debt);
                                                } else {
                                                    cancelAlarm(debt);
                                                }
                                            }
                                            // REMOVE: 07/09/2015 debtListAdapter.loadObjects();
//                                            if (iOweViewFragmentWithTag != null) {
//                                                iOweViewFragment.updateView();
//                                            }
//                                            if (oweMeViewFragment != null) {
//                                                oweMeViewFragment.updateView();
//                                            }
                                        }
                                    } else {
                                        Log.i("DebtListActivity",
                                                "Error pinning debts: "
                                                        + e.getMessage());
                                    }
                                }
                            });
                } else {
                    Log.i("DebtListActivity",
                            "loadFromParse: Error finding pinned debts: "
                                    + e.getMessage());
                }
            }
        });
    }

    /**
     * Sets a new notification alarm.
     *
     * @param debt with valid dueDate
     */
    private void setAlarm(Debt debt) {
        long timeInMillis = debt.getDueDate().getTime();
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.putExtra(Debt.KEY_TITLE, debt.getTitle());
        alertIntent.putExtra(Debt.KEY_OWNER_NAME, debt.getOwnerName());
        alertIntent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());

        alertIntent.setData(Uri.parse(ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, timeInMillis, PendingIntent.getBroadcast(
                this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(
                this,
                "Reminder  " + alarmId + " at "
                        + android.text.format.DateFormat.format(
                        "MM/dd/yy h:mmaa",
                        timeInMillis),
                Toast.LENGTH_LONG).show();// REMOVE: 07/09/2015
    }

    /**
     * Cancels notification alarm if exists.
     *
     * @param debt to cancel
     */
    private void cancelAlarm(Debt debt) {
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.setData(Uri.parse(ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(this, "REMOVED Reminder " + alarmId, Toast.LENGTH_LONG).show(); // REMOVE: 07/09/2015
    }


    private void updateLoggedInInfo() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser != null) {
            getSupportActionBar().setTitle(getString(R.string.logged_in_title,
                    currUser.getString(PARSE_USER_NAME_KEY)));
        } else {
            getSupportActionBar().setTitle(getResources().getString(R.string.not_logged_in_title));
        }
    }

    private void handleParseError(ParseException e) {
        handleInvalidSessionToken();// TODO: 05/09/2015

        /*        switch (e.getCode()) {
            case ParseException.INVALID_SESSION_TOKEN:
                handleInvalidSessionToken();
                break;

            // Other Parse API errors
        }*/
    }

    private void handleInvalidSessionToken() {
        //--------------------------------------
        // Option 1: Show a message asking the user to log out and log back in.// REMOVE: 06/09/2015
        //--------------------------------------
        // If the user needs to finish what they were doing, they have the opportunity to do so.
        //
        // new AlertDialog.Builder(getActivity())
        //   .setMessage("Session is no longer valid, please log out and log in again.")
        //   .setCancelable(false).setPositiveButton("OK", ...).create().show();

        //--------------------------------------
        // Option #2: Show login screen so user can re-authenticate.
        //--------------------------------------
        // You may want this if the logout button could be inaccessible in the UI.
        //
        // startActivityForResult(new ParseLoginBuilder(getActivity()).build(), 0);
        if (mIsShowLoginOnFail && !mWasSignupShowen) {
            // only in case the user initiated the sync - no demanding login
            mWasSignupShowen = true;
            openLoginView();
        } else {
/*            Toast.makeText(getApplicationContext(),
                    "Didn't show login\nisShowLoginOnFail: " + mIsShowLoginOnFail + "\nwasSignupShowen: " + mWasSignupShowen,
                    Toast.LENGTH_SHORT).show();// REMOVE: 06/09/2015*/
        }
    }

}