package com.yudaleh;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import com.parse.ParseException;
import com.parse.ParseUser;

/**
 * Accepts alarms on the due date of the debt.
 */
public class DueDateAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String uuid = intent.getData().getSchemeSpecificPart();
        String title = intent.getStringExtra(Debt.KEY_TITLE);
        String owner = intent.getStringExtra(Debt.KEY_OWNER_NAME);
        String phone = intent.getStringExtra(Debt.KEY_OWNER_PHONE);
        String tabTag = intent.getStringExtra(Debt.KEY_TAB_TAG);

        String firstPart;
        String preposition;
        if (tabTag.equals(Debt.OWE_ME_TAG)) {
            firstPart = "Get your ";
            preposition = " from ";
        } else {
            firstPart = "Return ";
            preposition = " to ";
        }

        // Raise a notification about the debt
        createNotification(context, firstPart + title, preposition + owner, title, uuid, owner, phone, tabTag);
    }

    /**
     * Creates and shows notification to the user.
     *
     * @param context app <code>Context</code> for the intent.
     * @param title   short content.
     * @param text    few more details.
     * @param alert   shows on the top bar for one second.
     * @param uuid    must be unique.
     * @param tabTag  should not be <code>null</code>.
     */
    private void createNotification(Context context, String title, String text, String alert, String uuid, String ownerName, String ownerPhone, String tabTag) {
        int alarmId = uuid.hashCode();

        Intent intent = new Intent(context, EditDebtActivity.class);
//        intent.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT*/ /*Intent.FLAG_ACTIVITY_SINGLE_TOP | */Intent.FLAG_ACTIVITY_CLEAR_TOP);// REMOVE: 21/09/2015
        intent.putExtra(Debt.KEY_UUID, uuid);
        intent.putExtra(Debt.KEY_TAB_TAG, tabTag);

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
        if (ownerPhone != null) {
            // Create dialing action
            String callTitle = "Call " + ownerName;
            int callIcon = R.drawable.ic_call_white_24dp;
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ownerPhone));
            PendingIntent notificationCallIntent = PendingIntent.getActivity(context, 0, callIntent
                    , PendingIntent.FLAG_UPDATE_CURRENT);

            // Create sms action
            int smsIcon = R.drawable.ic_sms_white_24dp;
            String smsTitle = "SMS " + ownerName;
            Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", ownerPhone, null));
            PendingIntent notificationSmsIntent = PendingIntent.getActivity(context, 0, smsIntent
                    , PendingIntent.FLAG_UPDATE_CURRENT);

/*            // Create chat action
            int chatIcon = R.drawable.ic_sms_white_24dp;
            String chatTitle = "Chat " + ownerName;
            PendingIntent notificationChatIntent = null;
            try {
                String objId = ParseUser.getQuery().whereEqualTo(MainActivity.PARSE_USER_PHONE_KEY, ownerPhone).find().get(0).getObjectId();
                Intent chatIntent = new Intent(context, MessagingActivity.class);
                chatIntent.putExtra("RECIPIENT_ID", objId);
                notificationChatIntent = PendingIntent.getActivity(context, 0, chatIntent
                        , PendingIntent.FLAG_UPDATE_CURRENT);
            } catch (ParseException e) {
                e.printStackTrace();
            }*/

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
              /*  if (notificationChatIntent != null) {
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
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(alarmId, notification);
    }

}
