package com.yudaleh;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.parse.FindCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.List;

/**
 * Adapter between the {@link com.fortysevendeg.swipelistview.SwipeListView} and the user's debts.
 */
class DebtSwipeListAdapter extends ArrayAdapter<Debt> {

    private final Context mContext;
    private final List<Debt> mDebts;
    private final int mResource;

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
        public ImageView debtImage;
        public TextView debtSubtitle;
        public Button action1;
        public Button action2;
        public Button action3;
    }


    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final Debt debt = mDebts.get(position);
        ViewHolder holder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.debtImage = (ImageView) view.findViewById(R.id.debt_icon);// TODO: 03/10/2015 rename
            holder.debtTitle = (TextView) view
                    .findViewById(R.id.debt_title);
            holder.debtSubtitle = (TextView) view.findViewById(R.id.debt_subtitle);// TODO: 03/10/2015 rename
            holder.action1 = (Button) view.findViewById(R.id.action_edit);
            holder.action2 = (Button) view.findViewById(R.id.action_message);
            holder.action3 = (Button) view.findViewById(R.id.action_call);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        TextView debtTitle = holder.debtTitle;
        TextView debtSubtitle = holder.debtSubtitle;
        ImageView debtImage = holder.debtImage;

        if (debt.getCurrencyPos() != Debt.NON_MONEY_DEBT_CURRENCY) {
            debtImage.setImageResource(R.drawable.dollar);
        } else {
            debtImage.setImageResource(R.drawable.box_closed_icon);// TODO: 25/09/2015 image / location
        }

        // Action 1:
        holder.action1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditView(debt);
            }
        });

        // Action 2:
        if (debt.getOwnerPhone() != null) {
            holder.action2.setText(R.string.action2_text_with_phone);
            holder.action2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showActionsDialog(debt);
                }
            });
        } else {
            holder.action2.setText(R.string.action2_text_no_phone);
            holder.action2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: 9/26/2015 set phone
                }
            });
        }
//        holder.action3.setText(mContext.getString(R.string.action3_text, debt.getDueDate()));
        // TODO: 30/09/2015 alarm on/off icon
        holder.action3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showDataTimePicker(debt);
            }
        });
        holder.action3.setVisibility(View.INVISIBLE);

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
        debtTitle.setText(debt.getTitle());
        if (debt.isDraft()) {
            debtTitle.setTypeface(null, Typeface.ITALIC);
            debtTitle.setTextColor(Color.RED);// TODO: 02/09/2015 GRAY

        } else {
            debtTitle.setTypeface(null, Typeface.NORMAL);
            if (debt.getStatus() == Debt.STATUS_CREATED) {
                debtTitle.setTextColor(Color.BLACK);
            } else if (debt.getStatus() == Debt.STATUS_PENDING) {
                debtTitle.setTextColor(Color.GREEN);
            } else if (debt.getStatus() == Debt.STATUS_CONFIRMED) {
                debtTitle.setTextColor(Color.BLUE);
            } else if (debt.getStatus() == Debt.STATUS_RETURNED) {
                debtTitle.setTextColor(Color.MAGENTA);
            } else {
                debtTitle.setTextColor(Color.YELLOW);
            }

        }

        debtSubtitle.setText(debt.getOwnerName());
        return view;
    }

    private void showDataTimePicker(final Debt debt) {
        SlideDateTimeListener listener = new SlideDateTimeListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onDateTimeSet(Date date) {
                date.setSeconds(0);
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
    private void showActionsDialog(final Debt debt) {
        int array;
        ParseUser currUser = ParseUser.getCurrentUser();
        if (currUser!=null) {
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
                                openConversationByPhone(debt);
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


    public void openConversationByPhone(Debt debt) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(MainActivity.PARSE_USER_PHONE_KEY, debt.getAuthorPhone());
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
    private void openEditView(Debt debt) {
        Intent i = new Intent(mContext, EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        mContext.startActivity(i);
    }

}
