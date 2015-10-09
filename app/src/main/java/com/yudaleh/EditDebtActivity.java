package com.yudaleh;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.google.gson.Gson;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SendCallback;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Choosing debt details. Used both for new and for existing debts.
 */
public class EditDebtActivity extends AppCompatActivity {

    private static final int FLAG_FORCE_BACK_TO_MAIN = 0x00040000;
    private static final int FLAG_SET_ALARM = 0X00020000;
    private static final int USER_EXISTENCE_CONFIRMED = 0X00060000;;
    private static final int USER_EXISTENCE_NOT_CONFIRMED = 0X00080000;;

    private Button remindButton;
    private CheckBox remindCheckBox;
    private CheckBox pushCheckBox;
    private EditText debtTitleText;
    private EditText debtOwnerText;
    private EditText debtPhoneText;
    private EditText debtDescText;
    private SearchView searchView;
    private AutoCompleteTextView searchAutoComplete;
    private ImageView closeBtn;

    private Spinner spinner1;
    private Debt debt;
    private String debtId;
    private String debtTabTag;
    private boolean isFromPush;
    private boolean isNew;
    private boolean isModified;

    private Debt beforeChange;
    private int currencyPos;


    //**********************************************************************************************
    //**************************************** Lifecycle methods: **********************************
    //**********************************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_debt);

        fetchExtras();
        setActionBarTitle();
        initViewHolders();
        try {
            prepareDebtForEditing();
        } catch (ParseException e) {
            e.printStackTrace();// REMOVE: 19/09/2015
            Toast.makeText(EditDebtActivity.this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra(Debt.KEY_TAB_TAG, debtTabTag);
            startActivity(intent);
        }

        if (isNew) {
            debtTitleText.requestFocus();
        }

        remindButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showDateTimePicker();
            }
        });
    }

    private void showDateTimePicker() {
        SlideDateTimeListener listener = new SlideDateTimeListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onDateTimeSet(Date date) {
                date.setSeconds(0);
                remindButton.setText(DateFormat.format("MM/dd/yy h:mmaa", date.getTime()));
                remindCheckBox.setChecked(true);
                debt.setDueDate(date);
            }

            @Override
            public void onDateTimeCancel() {

            }
        };
        Date now = new Date();
        Date prevDate = debt.getDueDate();
        Date initDate;
        if (prevDate != null && now.before(prevDate)) {
            // future scheduled date already exists
            initDate = prevDate;
        } else {
            initDate = now;
        }
        new SlideDateTimePicker.Builder(getSupportFragmentManager())
                .setListener(listener)
                .setInitialDate(initDate)
                .setIndicatorColor(getResources().getColor(R.color.accent_add))
                .build()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_debt, menu);
        setupSearch(menu);
        MenuItem deleteMenuItem = menu.findItem(R.id.action_delete);
        if (isNew) {
            deleteMenuItem.setVisible(false);
        } else {
            deleteMenuItem.setVisible(true);
        }
        if (isFromPush) {
            deleteMenuItem.setIcon(R.drawable.ic_cancel_white_36dp);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setDebtFieldsAfterEditing();
                if (isModified) {
                    showSaveChangesConfirm();
                    return true;
                } else {
                    cancelActivity();
                }
                break;
            case R.id.action_delete:// TODO: 24/09/2015 confirm dialog
                showDeleteConfirm();
                break;
            case R.id.action_done:
                if (!validateDebtDetails()) {
                    break;
                }
                setDebtFieldsAfterEditing();
                boolean isExistingUser = isExistingUser(debt.getOwnerPhone());

                if ((isNew || isModified) && debt.getOwnerPhone() != null) {

                    if (pushCheckBox.isChecked()) {// TODO: 24/09/2015 settings
                        if (isExistingUser) {
                            if (!isFromPush) {
                                sendPushToOwner();// TODO: 9/30/2015  auto receive push if modified (update existing)
                            }
                            showActionsDialog(true);
                        } else {
                            showNoPhoneErrorDialog();
                        }
                    } else {
                        showActionsDialog(false);
                    }

                } else {
                    saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                }
                break;
            default:
                break;
        }
        return false;
    }

    private void showNoPhoneErrorDialog() {
        (new AlertDialog.Builder(EditDebtActivity.this))
                .setMessage(R.string.no_phone_error)
                .setPositiveButton(R.string.skip_sync, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showActionsDialog(false);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteConfirm() {
        (new AlertDialog.Builder(EditDebtActivity.this))
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isExistingUser(debt.getOwnerPhone())) {
                            sendPushResponse(debt.getOtherUuid(), Debt.STATUS_DELETED);
                        }
                        cancelAlarm(debt);
                        // The debt will be deleted eventually but will immediately be excluded from mQuery results.
                        debt.deleteEventually();
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showSaveChangesConfirm() {
        (new AlertDialog.Builder(this))
                .setMessage(R.string.save_changes_confirm)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!validateDebtDetails()) {
                            return;
                        }
                        saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                        // TODO: 9/30/2015 send update push
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        revertChangesAndCancel();
                    }
                })
                .show();
    }

    static boolean isExistingUser(String phone) {
        if (phone == null) {
            return false;
        }
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(MainActivity.PARSE_USER_PHONE_KEY, phone);
        try {
            query.getFirst();
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Undo all changes and cancel the activity.
     */
    private void revertChangesAndCancel() {
        if (!isNew) {
            debt.copyFrom(beforeChange); // TODO: 9/30/2015 check if needed
        } // otherwise, changes are not saved anyway
        cancelActivity();
    }

    /**
     * Returns back home with cancel result.
     */
    private void cancelActivity() {
        setResult(RESULT_CANCELED);
        finish();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(Debt.KEY_TAB_TAG, debtTabTag);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        setDebtFieldsAfterEditing();
        if (isModified) {
            (new AlertDialog.Builder(this))
                    .setMessage(R.string.save_changes_confirm)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!validateDebtDetails()) {
                                return;
                            }
                            saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                            // TODO: 9/30/2015 send update push
                            EditDebtActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            revertChangesAndCancel();
                        }
                    })
                    .show();
        } else {
            cancelActivity();
        }
    }

    /**
     * Extracts the extras from the <code>Intent</code>.
     */
    private void fetchExtras() {
        debtId = getIntent().getStringExtra(Debt.KEY_UUID);
        debtTabTag = getIntent().getStringExtra(Debt.KEY_TAB_TAG);
        isFromPush = getIntent().getBooleanExtra("fromPush", false);
    }

    /**
     * Set the alarm and finish the activity.
     *
     * @param flags FLAG_SET_ALARM for setting the alarm if needed, and FLAG_FORCE_BACK_TO_MAIN for calling {@link MainActivity}.
     */
    private void wrapUp(int flags) {
        if ((flags & FLAG_SET_ALARM) != 0) {
            setAlarm(debt);
        }
        setResult(Activity.RESULT_OK);
        finish();
        if ((flags & FLAG_FORCE_BACK_TO_MAIN) != 0) {
            returnToMain(debt); // in case the activity was not started for a result
        }
    }

    /**
     * Opens {@link MainActivity}.
     *
     * @param debt for the intent's extras.
     */
    private void returnToMain(Debt debt) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivity(intent);
    }


    //**********************************************************************************************
    //**************************************** Debt operation methods: *****************************
    //**********************************************************************************************

    /**
     * Pins the <code>Debt</code> in local database and ? todo
     *
     * @param flags the <code>flags</code> parameter for the {@link #wrapUp}.
     */
    private void saveDebt(final int flags) {
        debt.pinInBackground(DebtListApplication.DEBT_GROUP_NAME,
                new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        if (isFinishing()) {
                            return;
                        }
                        if (e == null) {
                            if (isFromPush) {
                                sendPushResponse(debt.getOtherUuid(), Debt.STATUS_CONFIRMED);
                            }
                            wrapUp(flags);
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Error saving: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Updates the debt's details from the text fields.
     */
    private void setDebtFieldsAfterEditing() {
        debt.setTitle(debtTitleText.getText().toString());
        debt.setOwnerName(debtOwnerText.getText().toString());
        debt.setOwnerPhone(debtPhoneText.getText().toString(), getUserCountry(EditDebtActivity.this));
        debt.setDescription(debtDescText.getText().toString());
        if (!remindCheckBox.isChecked()) {
            // In case the date was already set by the dialog
            debt.setDueDate(null);
        }

        isModified = !debt.equals(beforeChange);

        ParseUser currUser = ParseUser.getCurrentUser();
//        debt.setAuthor(currUser);// REMOVE: 07/10/2015
        if (currUser != null) {
            debt.setAuthorName(currUser.getString(MainActivity.PARSE_USER_NAME_KEY));
            debt.setAuthorPhone(currUser.getString(MainActivity.PARSE_USER_PHONE_KEY));
        }
        debt.setStatus(Debt.STATUS_CREATED);
        debt.setCurrencyPos(currencyPos);// TODO: 06/10/2015 include in changes detection
        debt.setMoneyAmountByTitle();
        setTitleFormattedAsMoneyAmount(currencyPos);
        debt.setDraft(true);
    }

    /**
     * In case the debt is money, sets the title to: [money amount] [currency symbol].
     * Assumes the money amount was already set by {@link Debt#getMoneyAmount()}.
     *
     * @param currencyPos the position of the currency symbol in the <code>Spinner</code>.
     */
    private void setTitleFormattedAsMoneyAmount(int currencyPos) {
        if (currencyPos == Debt.NON_MONEY_DEBT_CURRENCY) {
            return;
        }
        String currency = spinner1.getItemAtPosition(currencyPos).toString();
        double amount = debt.getMoneyAmount();
        if (amount >= 0) {
            debt.setTitle(amount + " " + currency);
        } else {
            debt.setCurrencyPos(Debt.NON_MONEY_DEBT_CURRENCY);
        }
    }

    /**
     * Make sure all required fields are entered.
     *
     * @return <code>true</code> iff all fields are valid.
     */
    private boolean validateDebtDetails() {
        if (debtTitleText.getText().toString().trim().equals("")) {
            debtTitleText.setError(getString(R.string.no_title_error));
            requestViewFocus(debtTitleText);
            return false;
        }
        if (debtOwnerText.getText().toString().trim().equals("")) {
            debtOwnerText.setError(getString(R.string.no_owner_error));
            requestViewFocus(debtOwnerText);
            return false;
        }
        return true;
    }

    /**
     * Loads the current <code>Debt</code> from parse.
     *
     * @throws ParseException
     */
    private void loadExistingDebt() throws ParseException {
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo(Debt.KEY_UUID, debtId);
        debt = query.getFirst();
        debtTitleText.setText(debt.getTitle());
        spinner1.setSelection(debt.getCurrencyPos());
        debtOwnerText.setText(debt.getOwnerName());
        debtPhoneText.setText(debt.getOwnerPhone());
        debtDescText.setText(debt.getDescription());
        Date dueDate = debt.getDueDate();
        if (dueDate != null) {
            remindButton.setText(DateFormat.format("MM/dd/yy h:mmaa", dueDate.getTime()));
            remindCheckBox.setChecked(true);
        }
    }

    /**
     * Creates a copy of the current <code>Debt</code> with reversed tag.
     *
     * @throws ParseException
     */
    private void cloneDebtFromPush() throws ParseException {
        debt = new Debt();
        debt.setUuidString();

        ParseQuery<Debt> query = Debt.getQuery();
        query.whereEqualTo(Debt.KEY_UUID, debtId);

        Debt other = query.getFirst();
        debt.setOtherUuid(debtId);
        debt.setTabTag(reverseTag(other.getTabTag()));
        debtTitleText.setText(other.getTitle());
        debtOwnerText.setText(other.getAuthorName());
        debtPhoneText.setText(other.getAuthorPhone());
        debtDescText.setText(other.getDescription());
        Date dueDate = other.getDueDate();
        if (dueDate != null) {
            debt.setDueDate(dueDate);
            remindButton.setText(DateFormat.format("MM/dd/yy h:mmaa", dueDate.getTime()));
            remindCheckBox.setChecked(true);
        }
    }

    /**
     * Load the <code>Debt</code> from Parse or create a new one.
     *
     * @throws ParseException
     */
    private void prepareDebtForEditing() throws ParseException {
        if (isFromPush) {
            isNew = false;
            cloneDebtFromPush();
        } else if (debtId != null) {
            isNew = false;
            loadExistingDebt();
        } else {
            isNew = true;
            debt = new Debt();
            debt.setUuidString();
            debt.setTabTag(debtTabTag);
        }
        beforeChange = debt.createClone();
    }


    //**********************************************************************************************
    //**************************************** Communication methods: ******************************
    //**********************************************************************************************

    /**
     * Synchronize the status of the other end
     *
     * @param otherUuid of the debt on the destination side
     * @param status    to deliver to the other end
     */
    private void sendPushResponse(String otherUuid, final int status) {
        if (otherUuid == null) {
            return;
        }
        ParsePush push = new ParsePush();
        String phone = debt.getOwnerPhone();
        if (phone == null) {
            return;
        }
        push.setChannel(MainActivity.USER_CHANNEL_PREFIX + debt.getOwnerPhone().replaceAll("[^0-9]+", ""));
        // TODO: 06/10/2015  Gson gson = new Gson(); // Or use new GsonBuilder().create(); /*gson.toJson(o)*/
        // TODO: 14/09/2015 use proxy (add image, date): https://gist.github.com/janakagamini/f5c63ea27bee8b7b7581
        push.setMessage(status + "+" + debt.getUuidString() + "+" + otherUuid);
        push.sendInBackground(new SendCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    debt.setStatus(status);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Push not sent: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015
                }
            }
        });
    }

    /**
     * Shows a dialog with contact actions: call, sms, chat.
     *
     * @param isUserExistenceConfirmed whether {@link #isExistingUser(String)} already returned true
     */
    private void showActionsDialog(boolean isUserExistenceConfirmed) {
        int title;
        if (isNew) {
            title = R.string.contact_actions_dialog_title_new_debt;
        } else {
            title = R.string.contact_actions_dialog_title_modified_debt;
        }
        int array;
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser != null && (isUserExistenceConfirmed||isExistingUser(debt.getOwnerPhone()))) {
            array = R.array.contact_actions_array_logged_in;
        } else {
            array = R.array.contact_actions_array_logged_out;
        }
        String[] strArray = getResources().getStringArray(array);
        for (int i = 0; i < strArray.length; i++) {
            strArray[i] += " " + debt.getOwnerName();
        }
        (new AlertDialog.Builder(EditDebtActivity.this))
                .setTitle(title)
                .setItems(strArray, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (whichButton) {
                            case DebtListAdapter.ACTION_CHAT:
                                openConversationByPhone();
                                break;
                            case DebtListAdapter.ACTION_CALL:
                                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.getOwnerPhone()));
                                startActivity(dial);
                                break;
                            case DebtListAdapter.ACTION_SMS:
                                Intent sms = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", debt.getOwnerPhone(), null));
                                startActivity(sms);
                                break;

                        }
                        saveDebt(FLAG_SET_ALARM);
                    }
                })
                .setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                    }
                })
                .show();
    }

    public void openConversationByPhone() {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(MainActivity.PARSE_USER_PHONE_KEY, debt.getAuthorPhone());
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> user, com.parse.ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(getApplicationContext(), MessagingActivity.class);
                    intent.putExtra("RECIPIENT_ID", user.get(0).getObjectId());
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error finding that user",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Sends the owner a push notification about the debt.
     */
    private void sendPushToOwner() {
        // TODO: 14/09/2015 send only if data was changed
        ParsePush push = new ParsePush();
        String phone = debt.getOwnerPhone();
        if (phone == null) {
            return;
        }
        push.setChannel(MainActivity.USER_CHANNEL_PREFIX + phone.replaceAll("[^0-9]+", ""));
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        // TODO: 14/09/2015 use proxy (add image, date): https://gist.github.com/janakagamini/f5c63ea27bee8b7b7581
        push.setMessage(debt.getUuidString()/*gson.toJson(o)*/);///**/);
        push.sendInBackground(new SendCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    debt.setStatus(Debt.STATUS_PENDING);
                    // TODO: 16/09/2015 save
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Push not sent: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015

                }
            }
        });
    }


    //**********************************************************************************************
    //**************************************** Auxiliary methods: **********************************
    //**********************************************************************************************

    /**
     * Set the window's title according to the current tab tag.
     */
    private void setActionBarTitle() {//
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
/*        if (debtTabTag.equals(Debt.I_OWE_TAG)) {
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.i_owe_tab_title));
            }
        } else {
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.owe_me_tab_title));
            }
        }*/// REMOVE: 24/09/2015
    }

    /**
     * Next button listener that focuses on next empty <code>TextView</code>.
     */
    private TextView.OnEditorActionListener focusNextEmptyListener = new EditText.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusOnNextEmptyOrDone(v);
                return true;
            }
            return false;
        }
    };

    private void focusOnNextEmptyOrDone(TextView v) {
        TextView next = findNextEmptyView(v);
        if (next != null) {
            requestViewFocus(next);
        } else {
            clearViewFocus(v);
        }
    }

    /**
     * Retrieve the <code>View</code>s by their ids.
     */
    private void initViewHolders() {
        debtTitleText = (EditText) findViewById(R.id.debt_title);
        debtTitleText.setOnEditorActionListener(focusNextEmptyListener);
        debtOwnerText = (EditText) findViewById(R.id.debt_owner);
        debtOwnerText.setOnEditorActionListener(focusNextEmptyListener);
        debtPhoneText = (EditText) findViewById(R.id.debt_phone);
        debtPhoneText.setOnEditorActionListener(focusNextEmptyListener);
        debtPhoneText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().length() > 0) {
                    pushCheckBox.setChecked(true);
                    pushCheckBox.setVisibility(View.VISIBLE);
                } else {
                    pushCheckBox.setChecked(false);
                    pushCheckBox.setVisibility(View.GONE);
                }
            }
        });
        debtDescText = (EditText) findViewById(R.id.debt_desc);
        remindButton = (Button) findViewById(R.id.remind_button);
        remindCheckBox = (CheckBox) findViewById(R.id.remind_checkbox);
        pushCheckBox = (CheckBox) findViewById(R.id.push_checkbox);
        spinner1 = (Spinner) findViewById(R.id.spinner1);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currencyPos = position;
                if (position != Debt.NON_MONEY_DEBT_CURRENCY) {
                    debtTitleText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                } else {
                    debtTitleText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Request the focus on the given <code>TextView</code> the show the keyboard.
     *
     * @param v the out of focus <code>TextView</code>.
     */
    private void requestViewFocus(TextView v) {
        v.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Clear the focus on the given <code>TextView</code> the hide the keyboard.
     *
     * @param v the <code>TextView</code> in focus.
     */
    private void clearViewFocus(TextView v) {
        v.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * Switches to the opposite tag.
     *
     * @param tabTag the tag to reverse.
     * @return the opposite tag.
     */
    private String reverseTag(String tabTag) {
        if (tabTag.equals(Debt.I_OWE_TAG)) {
            tabTag = Debt.OWE_ME_TAG;
        } else {
            tabTag = Debt.I_OWE_TAG;
        }
        return tabTag;
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or <code>null</code> if not available).
     *
     * @param context <code>Context</code> reference to get the <code>TelephonyManager</code> instance from.
     * @return country code or <code>null</code>.
     */
    private static String getUserCountry(Context context) {// TODO: 21/09/2015 test on tablet
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                return simCountry.toUpperCase(Locale.US);
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toUpperCase(Locale.US);
                }
            } else {
//                Locale.Builder builder = new Locale.Builder();// TODO: 22/09/2015 for tablet / no network no sim
//                builder.setRegion(Locale.getDefault().getCountry());
//                Locale locale=builder.build();
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Returns the next empty <code>TextView</code>.
     *
     * @param view the <code>TextView</code> to start the search from (not including).
     * @return next empty field, or <code>null</code> if all fields are filled.
     */
    private TextView findNextEmptyView(TextView view) {
        int nextId = view.getNextFocusDownId();
        while (nextId != View.NO_ID) {
            TextView next = (TextView) findViewById(nextId);
            if (next.getText().toString().trim().equals("")) {
                return next;
            }
            nextId = next.getNextFocusDownId();
        }
        return null;
    }


    //**********************************************************************************************
    //**************************************** Contacts search methods: ****************************
    //**********************************************************************************************

    /**
     * Prepares the <code>SearchView</code>.
     *
     * @param menu the <code>Menu</code> from the parameter given to {@link #onCreateOptionsMenu}
     */
    private void setupSearch(Menu menu) {
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchViewMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchViewMenuItem.getActionView();
        setSearchTextColors();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        setSearchIcons();
    }

    /**
     * Changes the hint and text colors of the <code>SearchView</code>.
     */
    private void setSearchTextColors() {
        LinearLayout linearLayout1 = (LinearLayout) searchView.getChildAt(0);
        LinearLayout linearLayout2 = (LinearLayout) linearLayout1.getChildAt(2);
        LinearLayout linearLayout3 = (LinearLayout) linearLayout2.getChildAt(1);
        searchAutoComplete = (AutoCompleteTextView) linearLayout3.getChildAt(0);
        //Set the input text color
        searchAutoComplete.setTextColor(Color.WHITE);
        // set the hint text color
        searchAutoComplete.setHintTextColor(Color.WHITE);
        searchAutoComplete.setNextFocusDownId(R.id.debt_title);
        searchAutoComplete.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    closeSearchView();
                    focusOnNextEmptyOrDone(v);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Changes the icons of the <code>SearchView</code>, using Java's reflection.
     */
    private void setSearchIcons() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            closeBtn = (ImageView) searchField.get(searchView);
            closeBtn.setImageResource(R.drawable.ic_close_white_36dp);

            searchField = SearchView.class.getDeclaredField("mVoiceButton");
            searchField.setAccessible(true);
            ImageView voiceBtn = (ImageView) searchField.get(searchView);
            voiceBtn.setImageResource(R.drawable.ic_keyboard_voice_white_36dp);

            searchField = SearchView.class.getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchButton = (ImageView) searchField.get(searchView);
            searchButton.setImageResource(R.drawable.ic_search_white_36dp);

            // Accessing the SearchAutoComplete
            int queryTextViewId = getResources().getIdentifier("android:id/search_src_text", null, null);
            View autoComplete = searchView.findViewById(queryTextViewId);

            Class<?> clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");

            SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
            stopHint.append(getString(R.string.findContact));

            // Add the icon as an spannable
            Drawable searchIcon = getResources().getDrawable(R.drawable.ic_search_white_36dp);
            Method textSizeMethod = clazz.getMethod("getTextSize");
            Float rawTextSize = (Float) textSizeMethod.invoke(autoComplete);
            int textSize = (int) (rawTextSize * 1.25);
            if (searchIcon != null) {
                searchIcon.setBounds(0, 0, textSize, textSize);
            }
            stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the new hint text
            Method setHintMethod = clazz.getMethod("setHint", CharSequence.class);
            setHintMethod.invoke(autoComplete, stopHint);

        } catch (NoSuchFieldException e) {
            Log.e("SearchView", e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e("SearchView", e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Handle the search intent
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            // Suggestion clicked mQuery
            String displayName = getDisplayNameForContact(intent);
            debtOwnerText.setText(displayName);
            String phone = getPhoneNumber(displayName);
            debtPhoneText.setText(phone);
            searchAutoComplete.onEditorAction(EditorInfo.IME_ACTION_NEXT);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) { // REMOVE: 14/09/2015
            // Other query
            searchAutoComplete.onEditorAction(EditorInfo.IME_ACTION_NEXT);
        }
    }

    private void closeSearchView() {
        closeBtn.performClick();
        closeBtn.performClick();
    }

    /**
     * Get contact's display name by the search mQuery.
     *
     * @param intent the <code>Intent</code> the started the search.
     * @return contact's display name.
     */
    private String getDisplayNameForContact(Intent intent) {
        Cursor phoneCursor = getContentResolver().query(intent.getData(), null, null, null, null);
        phoneCursor.moveToFirst();
        int idDisplayName = phoneCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        String name = phoneCursor.getString(idDisplayName);
        phoneCursor.close();
        return name;
    }

    /**
     * Extract the phone number by display name.
     *
     * @param name contacts display name.
     * @return the phone number of the contact.
     */
    private String getPhoneNumber(String name) {
        String ret = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like'" + name + "'";
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if (c.moveToFirst()) {
            ret = c.getString(0);
        }
        c.close();
        if (ret == null) {
            ret = "Unsaved";
        }
        return ret;
    }


    //**********************************************************************************************
    //**************************************** Alarm methods: **************************************
    //**********************************************************************************************

    /**
     * Sets a new notification alarm.
     * In case the due date is null the alarm is canceled.
     *
     * @param debt with valid dueDate.
     */
    private void setAlarm(Debt debt) {
        Date dueDate = debt.getDueDate();
        if (dueDate == null) {
            cancelAlarm(debt);
            return;
        }
        long timeInMillis = dueDate.getTime();
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.putExtra(Debt.KEY_TITLE, debt.getTitle());
        alertIntent.putExtra(Debt.KEY_OWNER_NAME, debt.getOwnerName());
        alertIntent.putExtra(Debt.KEY_OWNER_PHONE, debt.getOwnerPhone());
        alertIntent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());

        alertIntent.setData(Uri.parse(MainActivity.ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, timeInMillis, PendingIntent.getBroadcast(
                this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(this, "Reminder  " + alarmId + " at "
                        + DateFormat.format("MM/dd/yy h:mmaa", timeInMillis),
                Toast.LENGTH_LONG).show();// REMOVE: 07/09/2015
    }

    /**
     * Cancels notification alarm if exists.
     *
     * @param debt to cancel.
     */
    private void cancelAlarm(Debt debt) {
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.setData(Uri.parse(MainActivity.ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(this, "REMOVED Reminder " + alarmId, Toast.LENGTH_LONG).show(); // REMOVE: 07/09/2015
    }

}
