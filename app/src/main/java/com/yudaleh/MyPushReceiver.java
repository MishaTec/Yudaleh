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
import android.support.v4.content.LocalBroadcastManager;

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
    private String debtId = null;
    private int debtStatus = Debt.STATUS_NO_STATUS;
    private boolean isResponsePush = false;
    private boolean isNew = true;
    private boolean isReturned = false;
    private Intent broadcastIntent = new Intent("com.yudaleh.MainActivity");
    private LocalBroadcastManager broadcaster;
    private String senderName;
    private String senderPhone;
    private boolean isChatPush = false;
    private String senderId;
    private String personPhone;
    private String personName;


    @Override
    public void onPushReceive(final Context context, Intent intent) {
        String title = null;
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(intent.getStringExtra(MyPushReceiver.KEY_PUSH_DATA));
            if (jsonObject.has("isNew")) {
                isNew = jsonObject.getBoolean("isNew");
            }
            if (jsonObject.has("isResponsePush")) {

                isResponsePush = jsonObject.getBoolean("isResponsePush");
            }
            if (jsonObject.has("isChatPush")) {
                isChatPush = jsonObject.getBoolean("isChatPush");
            }
            if (jsonObject.has("SENDER_ID")) {
                senderId = jsonObject.getString("SENDER_ID");
            }
            if (jsonObject.has("SENDER_NAME")) {

                senderName = jsonObject.getString("SENDER_NAME");
            }     
            if (jsonObject.has("SENDER_PHONE")) {

                senderPhone = jsonObject.getString("SENDER_PHONE");
            }
            if (jsonObject.has("title")) {

                title = jsonObject.getString("title");
            }
            if (jsonObject.has(Debt.KEY_IS_RETURNED)) {
                isReturned = jsonObject.getBoolean(Debt.KEY_IS_RETURNED);
            }
            if (jsonObject.has(Debt.KEY_STATUS)) {
                debtStatus = jsonObject.getInt(Debt.KEY_STATUS);
            }
            if (jsonObject.has(Debt.KEY_UUID)) {
                debtOtherId = jsonObject.getString(Debt.KEY_UUID);
            }
            if (jsonObject.has(Debt.KEY_OTHER_UUID)) {
                debtId = jsonObject.getString(Debt.KEY_OTHER_UUID);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (isChatPush) {
            createNotification(context, senderName, title, senderName + ": " + title);
        }
        broadcaster = LocalBroadcastManager.getInstance(context);
        if (!isResponsePush) {
            if (isNew) {
                title = "Debt created";
            } else {
                title = "Debt modified";
            }
        }

        ParseQuery<Debt> query = Debt.getQuery();
        if (isResponsePush) {
            query.fromLocalDatastore();
            query.whereEqualTo(Debt.KEY_UUID, debtId);
        } else {
            query.whereEqualTo(Debt.KEY_UUID, debtOtherId);
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
                    if (debtStatus != Debt.STATUS_NO_STATUS) {
                        debt.setStatus(debtStatus);// TODO: 16/09/2015 save
                    }
                    debt.setReturned(isReturned);
                    debt.setOtherUuid(debtOtherId);// TODO: 16/09/2015 save
                    if (isReturned) {
                        debt.setDueDate(null); // TODO: 12/10/2015 confirm dialog
                        cancelAlarm(context, debt);
                    }

                    debt.setDraft(true);
                    debt.pinInBackground(DebtListApplication.DEBT_GROUP_NAME, new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                broadcastIntent.putExtra("update", true);
                                broadcaster.sendBroadcast(broadcastIntent);
                            }
                        }
                    });
                }
                if (pushTitle != null && !pushTitle.equals(EditDebtActivity.PUSH_TITLE_RESPONSE)) {
                    createNotification(context, pushTitle, pushText, pushAlert);
                }
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
        Intent intent;
        int alarmId;

        if (isChatPush) {
            intent = new Intent(context, MainActivity.class);
            intent.putExtra("SENDER_ID", senderId);
            intent.putExtra("isChatPush", true);
            intent.putExtra("fromPush", true);
            intent.putExtra(Debt.KEY_TAB_TAG, Debt.I_OWE_TAG);
            intent.putExtra("isNew", false);
            alarmId = senderId.hashCode();
            personPhone = senderPhone;
            personName = senderName;
        } else {
            personPhone = debt.getAuthorPhone();
            personName = debt.getAuthorName();
            String uuidString = debt.getUuidString();
            alarmId = uuidString.hashCode();
            intent = new Intent(context, EditDebtActivity.class);
            intent.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT*/ Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);// REMOVE: 14/09/2015
            intent.putExtra(Debt.KEY_UUID, debtId);
            intent.putExtra(Debt.KEY_OTHER_UUID, debtOtherId);
            intent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
            intent.putExtra("fromPush", true);
            intent.putExtra("isNew", isNew);
        }
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

        if (personPhone != null) {
            // Create dialing action
            String callTitle = "Call " + personName;
            int callIcon = R.drawable.ic_call_white_24dp;
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + personPhone));
            PendingIntent notificationCallIntent = PendingIntent.getActivity(context, 0, callIntent
                    , PendingIntent.FLAG_UPDATE_CURRENT);

            // Create sms action
            int smsIcon = R.drawable.ic_sms_white_24dp;
            String smsTitle = "SMS " + personName;
            Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", personPhone, null));
            PendingIntent notificationSmsIntent = PendingIntent.getActivity(context, 0, smsIntent
                    , PendingIntent.FLAG_UPDATE_CURRENT);

            // Create chat action
/*            int chatIcon = R.drawable.ic_chat_white_24dp;
            String chatTitle = "Chat " + personName;
            PendingIntent notificationChatIntent = null;
            try {
                String objId = ParseUser.getQuery().whereEqualTo(MainActivity.PARSE_USER_PHONE_KEY, personPhone).find().get(0).getObjectId();
                Intent chatIntent = new Intent(context, MessagingActivity.class);
                chatIntent.putExtra("RECIPIENT_ID", objId);
                notificationChatIntent = PendingIntent.getActivity(context, 0, chatIntent
                        , PendingIntent.FLAG_UPDATE_CURRENT);
            } catch (ParseException e) {
                e.printStackTrace();
            }*/

            // Add all actions
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
/*                if (notificationChatIntent != null) {
                    builder.addAction(new Notification.Action.Builder(
                            Icon.createWithResource(context, chatIcon),
                            chatTitle,
                            notificationChatIntent)
                            .build());
                }*/
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
