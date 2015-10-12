package com.yudaleh;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Creates notification for the received debt, which allows the user to add/ignore it
 */
public class MyPushReceiver extends ParsePushBroadcastReceiver {
    private Debt debt;
    private String debtOtherId = null;
    private int debtStatus;
    private boolean isResponsePush = false;
    private boolean isNew = true;

    @Override
    public void onPushReceive(final Context context, Intent intent) {
        String alert = null;
        String title = null;
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(intent.getStringExtra(MyPushReceiver.KEY_PUSH_DATA));
            isNew = jsonObject.getBoolean("isNew");
            isResponsePush = jsonObject.getBoolean("isResponsePush");
            title = jsonObject.getString("title");
            alert = jsonObject.getString("alert");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (title == null || alert == null) {
            return;
        }
        if (!isResponsePush) {
            if (title.equals(EditDebtActivity.PUSH_TITLE_NEW)) {
                title = "Debt created";
            } else {
                title = "Debt modified";
            }
        }
        String[] alertParts = alert.split("\\+");
        final String debtId;
        if (alertParts.length == 3) {
            debtStatus = Integer.parseInt(alertParts[0]);
            debtOtherId = alertParts[1];
            debtId = alertParts[2];
        } else {
            debtId = alert;// REMOVE: 12/10/2015
        }
        ParseQuery<Debt> query = Debt.getQuery();
        query.whereEqualTo(Debt.KEY_UUID, debtOtherId);
        if (isResponsePush) {
            query.fromLocalDatastore();
        }
        final String pushAlert = title;
        query.getFirstInBackground(new GetCallback<Debt>() {// TODO: 12/10/2015 transfer all by JSON (only main keys - others will be cloned on edit)

            @Override
            public void done(Debt object, ParseException e) {
                if (e != null) {
                    System.err.println("onPushReceive fail: " + e.getMessage());
                    return;
                }
                debt = object;
                String pushText = debt.getTitle();
                String pushTitle;
                if (debt.getTabTag().equals(Debt.OWE_ME_TAG)) { // reversed logic
                    pushTitle = "You owe " + debt.getAuthorName();
                } else {
                    pushTitle = debt.getAuthorName() + " owes you";
                }
                if (isResponsePush) { //remove notification
                    pushTitle = pushAlert;
                    pushText = null;
                    debt.setStatus(debtStatus);// TODO: 16/09/2015 save
                    debt.setOtherUuid(debtOtherId);// TODO: 16/09/2015 save
                    if (debtStatus == Debt.STATUS_RETURNED) {
                        debt.setDueDate(null); // TODO: 12/10/2015 confirm dialog
                        cancelAlarm(context, debt);
                    }

                    debt.setDraft(true);
                    debt.pinInBackground(DebtListApplication.DEBT_GROUP_NAME, new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Intent intent = new Intent(context, MainActivity.class);
                                intent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
                                intent.putExtra("isResponsePush", true);
                                // todo use broadcast, or update current if opened (same fragment)
                            }
                        }
                    });
                }
                createNotification(context, pushTitle, pushText, pushAlert);
            }

        });
    }

    /**
     * Creates and shows notification to the user.
     *
     * @param context app context for the intent
     * @param title   short content
     * @param text    few more details
     * @param alert   shows on the top bar for one second
     */
    private void createNotification(Context context, String title, String text, String alert) {
        String uuidString = debt.getUuidString();
        int alarmId = uuidString.hashCode();
        Intent intent = new Intent(context, EditDebtActivity.class);
//        intent.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT*/ /*Intent.FLAG_ACTIVITY_SINGLE_TOP | */Intent.FLAG_ACTIVITY_CLEAR_TOP);// REMOVE: 14/09/2015
        intent.putExtra(Debt.KEY_UUID, debtOtherId);
        intent.putExtra(Debt.KEY_OTHER_UUID, uuidString);
        intent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        intent.putExtra("fromPush", true);
        intent.putExtra("isNew", isNew);

        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0, intent
                , PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(title)
                .setTicker(alert)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(notificationIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true);
        addContactActions(context, builder);
        Notification notification = builder.build();
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(alarmId, notification); // make sure alarmId is unique
    }

    private void addContactActions(Context context, Notification.Builder builder) {
        String authorPhone = debt.getAuthorPhone();
        String authorName = debt.getAuthorName();
        if (authorPhone != null) {
            // Create dialing action
            String callTitle = "Call " + authorName;
            int callIcon = R.drawable.ic_call_white_36dp;
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + authorPhone));
            PendingIntent notificationCallIntent = PendingIntent.getActivity(context, 0, callIntent
                    , PendingIntent.FLAG_UPDATE_CURRENT);

            // Create sms action
            int smsIcon = R.drawable.ic_call_white_36dp;
            String smsTitle = "SMS " + authorName;
            Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", authorPhone, null));
            PendingIntent notificationSmsIntent = PendingIntent.getActivity(context, 0, smsIntent
                    , PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.addAction(new Notification.Action.Builder(
                        Icon.createWithResource(context, callIcon),
                        callTitle,
                        notificationCallIntent)
                        .build());
                builder.addAction(new Notification.Action.Builder(
                        Icon.createWithResource(context, smsIcon),
                        smsTitle,
                        notificationSmsIntent)
                        .build());
            } else {
                //noinspection deprecation
                builder.addAction(callIcon, callTitle, notificationCallIntent);
                //noinspection deprecation
                builder.addAction(smsIcon, smsTitle, notificationSmsIntent);
            }
        }
    }

    /**
     * Cancels notification alarm if exists.
     *
     * @param debt to cancel
     */
    private void cancelAlarm(Context context, Debt debt) {
        Intent alertIntent = new Intent(context, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.setData(Uri.parse(MainActivity.ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(context, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

    }
}
