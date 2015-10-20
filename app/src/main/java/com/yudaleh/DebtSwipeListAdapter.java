package com.yudaleh;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SendCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Adapter between the {@link com.fortysevendeg.swipelistview.SwipeListView} and the user's debts.
 */
class DebtSwipeListAdapter extends ArrayAdapter<Debt> {

    private final Context mContext;
    private final List<Debt> mDebts;
    private final int mResource;


    static final int RESPONSE_CODE_TOGGLE_RETURNED = 0;
    static final int RESPONSE_CODE_DUE_DATE_CHANGED = 1;

    static final int ACTION_CALL = 0;
    static final int ACTION_SMS = 1;
    static final int ACTION_CHAT = 2;

    public DebtSwipeListAdapter(Context context, int resource, List<Debt> debts) {
        super(context, resource, debts);
        this.mDebts = debts;
        this.mContext = context;
        this.mResource = resource;
    }

    private class ViewHolder {
        TextView debtTitle;
        public ParseImageView debtImage;
        public TextView debtSubtitle;
        public Button action1;
        public Button action2;
        public Button action3;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final Debt debt = mDebts.get(position);
        ViewHolder holder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.debtImage = (ParseImageView) view.findViewById(R.id.debt_icon);// TODO: 03/10/2015 rename
            holder.debtTitle = (TextView) view
                    .findViewById(R.id.debt_title);
            holder.debtSubtitle = (TextView) view.findViewById(R.id.debt_subtitle);// TODO: 03/10/2015 rename
            holder.action1 = (Button) view.findViewById(R.id.action1);
            holder.action2 = (Button) view.findViewById(R.id.action2);
            holder.action3 = (Button) view.findViewById(R.id.action3);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        TextView debtTitle = holder.debtTitle;
        TextView debtSubtitle = holder.debtSubtitle;
        final ParseImageView debtIcon = holder.debtImage;
        final ParseFile thumbFile = debt.getThumbFile();
        final ParseFile photoFile = debt.getPhotoFile();

        if (debt.isReturned()) {
            debtIcon.setImageResource(R.drawable.ic_done_black_48dp);
        } else if (thumbFile != null) {
            debtIcon.setParseFile(thumbFile);
            debtIcon.loadInBackground();
            debtIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fragmentManager = ((FragmentActivity) mContext).getSupportFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    FullscreenFragment fragment = new FullscreenFragment();
                    fragment.setArguments(debtIcon, photoFile);
                    transaction.add(android.R.id.content, fragment, MainActivity.FULLSCREEN_FRAGMENT_TAG);
                    transaction.commit();
                }
            });
        } else if (debt.getCurrencyPos() != Debt.NON_MONEY_DEBT_CURRENCY) {
            debtIcon.setImageResource(R.drawable.dollar);
        } else {
            debtIcon.setImageResource(R.drawable.box_closed_icon);// TODO: 25/09/2015 image / location
        }

        // Action 1:
        holder.action1.setText(debt.isReturned() ? R.string.action1_text_returned : R.string.action1_text);
        holder.action1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debt.toggleReturned();
                debt.setDraft(true);
                try {
                    debt.pin(DebtListApplication.DEBT_GROUP_NAME);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                sendPushResponse(debt, RESPONSE_CODE_TOGGLE_RETURNED);
                notifyDataSetChanged();
            }
        });

        // Action 2:
        if (debt.getOwnerPhone() != null) {
            holder.action2.setText(R.string.action2_text_with_phone);
            holder.action2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showActionsDialog(debt, EditDebtActivity.CODE_USER_EXISTENCE_NOT_CHECKED);
                }
            });
        } else {
            holder.action2.setText(R.string.action2_text_no_phone);
            holder.action2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openEditViewWithFocusOnPhone(debt);// TODO: 11/10/2015 just set phone? or we do want the search feature
                }
            });
        }
//        holder.action3.setText(mContext.getString(R.string.action3_text, debt.getDueDate()));
        // TODO: 30/09/2015 alarm on/off icon
        holder.action3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimePicker(debt);
            }
        });

        // TODO: 05/09/2015 remove info
/*
        ParseUser author = debt.getAuthor();
        if(author!=null) {
            String token = author.getSessionToken();
            boolean isAuth = author.isAuthenticated();
            boolean isDataAvai = author.isDataAvailable();
            boolean isNew = author.isNew();
            boolean isDirty = author.isDirty();
            boolean isLinked = ParseAnonymousUtils.isLinked(author);
        }
//            String info = "\nauthor: "+author.getUsername()+"\nisAuth: "+isAuth+"\nisDataAvai: "+isDataAvai+"\nisNew: "+isNew+"\nisDirty: "+isDirty+"\ntoken: "+token+"\nisLinked: "+isLinked;
*/

//String extra = "\n"+debt.getUuidString()+"<-"+debt.getOtherUuid(); // REMOVE: 24/09/2015
        String titleStr = debt.getTitle();

        if (debt.isDraft()) {
            debtTitle.setTypeface(null, Typeface.ITALIC);
            debtTitle.setTextColor(Color.GRAY);
        } else {
            debtTitle.setTypeface(null, Typeface.NORMAL);
            debtTitle.setTextColor(Color.BLACK);
        }

        if (debt.isReturned()) {
            debtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_all_black_24dp, 0);
        } else if (debt.getStatus() == Debt.STATUS_CONFIRMED) {
            debtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_black_24dp, 0);
        } else if (debt.getStatus() == Debt.STATUS_PENDING) {
            debtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_access_time_black_24dp, 0);
        } else if (debt.getStatus() == Debt.STATUS_DENIED) {
            titleStr += " (denied)";
            debtTitle.setTextColor(Color.RED);
            debtTitle.setTypeface(null, Typeface.BOLD);
            debtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_block_black_24dp, 0);
        } else if (debt.getStatus() == Debt.STATUS_DELETED) {
            titleStr += " (deleted)";
            debtTitle.setTextColor(Color.RED);
            debtTitle.setTypeface(null, Typeface.BOLD);
            debtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_outline_black_24dp, 0);
        }
        debtTitle.setText(titleStr);

/*        String created = null;
        Date createdDate = debt.getDateCreated();
        if(createdDate!=null) {
            created = DateFormat.format("MM/dd/yy", debt.getDateCreated().getTime()).toString();
        }*/// TODO: 10/10/2015 put in edit mode
        boolean isOutdatedDebt = false;
        String due = null;
        Date dueDate = debt.getDueDate();
        if (dueDate != null) {
            due = DateFormat.format("MM/dd/yy h:mmaa", dueDate.getTime()).toString();
            if (dueDate.before(new Date())) {
                due += " (outdated)";
                isOutdatedDebt = true;
            }
        }
        String subtitle = (due != null ? "Due " + due : "No due date");
        debtSubtitle.setText(subtitle);
        if (isOutdatedDebt) {
            debtSubtitle.setTextColor(Color.RED);
        }
        return view;
    }


    /**
     * Synchronize the status of the other end
     */
    private void sendPushResponse(Debt debt, int responseCode) {
        String ownerPhone = debt.getOwnerPhone();
        String otherUuid = debt.getOtherUuid();
        if (ownerPhone == null) {
            return;
        }
        if (otherUuid == null) {
            return;
        }

        JSONObject jsonObject;
        try {
            String title;
            switch (responseCode) {
                case RESPONSE_CODE_TOGGLE_RETURNED:
                    title = debt.getOwnerName() + " marked " + debt.getTitle() + " as " + (debt.isReturned() ? "" : "not ") + "returned";
                    break;
                case RESPONSE_CODE_DUE_DATE_CHANGED:
                    title = debt.getOwnerName() + " changed due date of: " + debt.getTitle();
                    break;
                default:
                    return;
            }
            jsonObject = new JSONObject();
            jsonObject.put("title", title);
            jsonObject.put(Debt.KEY_IS_RETURNED, debt.isReturned());
            jsonObject.put(Debt.KEY_STATUS, debt.getStatus());
            jsonObject.put(Debt.KEY_UUID, debt.getUuidString());
            jsonObject.put(Debt.KEY_OTHER_UUID, otherUuid);
            jsonObject.put("isNew", false);
            jsonObject.put("isResponsePush", true);

            ParsePush push = new ParsePush();
            push.setChannel(MainActivity.USER_CHANNEL_PREFIX + ownerPhone.replaceAll("[^0-9]+", ""));
            // TODO: 06/10/2015  Gson gson = new Gson(); // Or use new GsonBuilder().create(); /*gson.toJson(o)*/
            // TODO: 14/09/2015 use proxy (add image, date): https://gist.github.com/janakagamini/f5c63ea27bee8b7b7581
            push.setData(jsonObject);
            push.sendInBackground(new SendCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                    } else {
                        Toast.makeText(mContext,
                                "Push not sent: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void showDateTimePicker(final Debt debt) {
        SlideDateTimeListener listener = new SlideDateTimeListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onDateTimeSet(Date date) {
                date.setSeconds(0);
                debt.setDueDate(date);// TODO: 11/10/2015 save debt
                debt.setDraft(true);
                try {
                    debt.pin(DebtListApplication.DEBT_GROUP_NAME);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                sendPushResponse(debt, RESPONSE_CODE_DUE_DATE_CHANGED);
                notifyDataSetChanged();
            }

            @Override
            public void onDateTimeCancel() {
// TODO: 10/17/2015 remove dueDate, but change button text to "Remove"
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
        new SlideDateTimePicker.Builder(((AppCompatActivity) mContext).getSupportFragmentManager())
                .setListener(listener)
                .setInitialDate(initDate)
                .setIndicatorColor(mContext.getResources().getColor(R.color.accent_add))
                .build()
                .show();
    }


    /**
     * Show a confirmation push notification dialog, with an option to call the owner.
     */
    private void showActionsDialog(final Debt debt, int code) {
        int array;
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser != null && EditDebtActivity.isExistingUser(debt.getOwnerPhone(), code)) {
            array = R.array.contact_actions_array_logged_in;
        } else {
            array = R.array.contact_actions_array_logged_out;
        }
        (new AlertDialog.Builder(mContext))
                .setTitle(R.string.contact_actions_dialog_title_action)
                .setItems(array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (whichButton) {
                            case ACTION_CHAT:
                                openConversationByPhone(debt.getOwnerPhone());
                                break;
                            case ACTION_CALL:
                                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.getOwnerPhone()));
                                mContext.startActivity(dial);
                                break;
                            case ACTION_SMS:
                                Intent sms = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", debt.getOwnerPhone(), null));
                                mContext.startActivity(sms);
                                break;
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void openConversationByPhone(String phone) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(MainActivity.PARSE_USER_PHONE_KEY, phone);
        query.findInBackground(new FindCallback<ParseUser>() {// TODO: 07/10/2015 getFirst
            public void done(List<ParseUser> users, com.parse.ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(mContext, MessagingActivity.class);
                    intent.putExtra("RECIPIENT_ID", users.get(0).getObjectId());
                    mContext.startActivity(intent);
                } else {
                    Toast.makeText(mContext,
                            "Error finding that user",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Helper methods: -----------------------------------------------------------------------------
    private void openEditViewWithFocusOnPhone(Debt debt) {
        Intent i = new Intent(mContext, EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        i.putExtra(EditDebtActivity.FOCUS_ON_PHONE_EXTRA, true);
        mContext.startActivity(i);
    }

}
