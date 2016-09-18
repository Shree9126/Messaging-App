package com.app.infideap.readcontact.controller.access.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.app.infideap.readcontact.R;
import com.app.infideap.readcontact.controller.access.ui.activity.ChatActivity;
import com.app.infideap.readcontact.controller.access.ui.activity.MainActivity;
import com.app.infideap.readcontact.entity.Contact;
import com.app.infideap.readcontact.entity.Data;
import com.app.infideap.readcontact.util.Common;
import com.app.infideap.readcontact.util.Constant;
import com.app.infideap.readcontact.util.References;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Shiburagi on 12/09/2016.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private final References references = References.getInstance();
    private static Query query;
    private boolean sound;

    //    public static android.support.v4.app.NotificationCompat.InboxStyle inboxStyle;
//    public static TreeMap<String, RemoteMessage> hashMap = new TreeMap<>();

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        sound = true;
        if (query == null)
            displayNotification(remoteMessage);
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void updateStatus(final DatabaseReference databaseReference, Data data) {

        Log.d(TAG, data.chatKey + "," + data.key);


//        hashMap.put(key, remoteMessage);


        final DatabaseReference reference = references.getChat().message(
                data.chatKey
        ).child(data.key).child(Constant.STATUS);
        reference
                .setValue(Constant.MESSAGE_RECEIVE)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.getException() == null)
                            return;

                        task.getException().printStackTrace();
                    }
                });
        reference.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null)
                            return;

                        if (dataSnapshot.getValue(Integer.class) == Constant.MESSAGE_READ) {
                            databaseReference.setValue(null);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );


    }

    private Data convert(RemoteMessage remoteMessage) {
        JSONObject object = new JSONObject(remoteMessage.getData());
        GsonBuilder builder = new GsonBuilder();

        Gson gson = builder.create();

        return gson.fromJson(object.toString(), Data.class);
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param data FCM message body received.
     */
    private void sendNotification(List<Data> datas, final Data data) {


//        String from = messageBody.getNotification().getTitle();
//        from = from == null ? "<>" : from;
//        String body = String.format(
//                Locale.getDefault(),
//                "%s : %s",
//                from,
//                messageBody.getNotification().getBody()
//        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("")
                .setAutoCancel(true);
        if (sound)
            notificationBuilder.setSound(defaultSoundUri);

        sound = false;

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        Contact contactDetail = Common.contactDetail(this, data.sender.substring(data.countryCode.length() + 1));
        String from;
        if (contactDetail == null) {
            from = data.sender;
            contactDetail = new Contact();
            contactDetail.name = from;

        } else {
            from = contactDetail.name;

        }
        contactDetail.phoneNumber = data.sender;

        from = from == null ? "<>" : from;


        PendingIntent pendingIntent;

        boolean isSingleSender = true;
        String prevFrom = from;
        for (Data _data : datas) {

            Contact contact = Common.contactDetail(this, _data.sender.substring(data.countryCode.length() + 1));
            if (contact == null)
                from = _data.sender;
            else
                from = contact.name;
            from = from == null ? "<>" : from;
            String body = String.format(
                    Locale.getDefault(),
                    "%s : %s",
                    from,
                    _data.message
            );
            inboxStyle.addLine(body);

            if (!prevFrom.equals(from))
                isSingleSender = false;
        }

        if (isSingleSender) {
            String body = String.format(
                    Locale.getDefault(),
                    "%s : %s",
                    from,
                    data.message
            );


            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.CONTACT, contactDetail);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            notificationBuilder.setContentTitle(from)
                    .setContentIntent(pendingIntent);
            notificationBuilder.setContentText(body);


        } else {

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            notificationBuilder.setContentTitle(getResources().getString(R.string.app_name))
                    .setContentIntent(pendingIntent);


        }

        notificationBuilder.setStyle(inboxStyle);

//        notificationBuilder.setStyle(displayNotification(messageBody));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

        return;
    }

    private NotificationCompat.InboxStyle displayNotification(final RemoteMessage messageBody) {
        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        query = references.getUser().notification(Common.getSimSerialNumber(this))
                .child(Constant.ITEMS).orderByChild(Constant.DATETIME);
        query
                .addChildEventListener(
                        new ChildEventListener() {
                            List<Data> datas = new ArrayList<Data>();

                            @Override
                            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                                final Data data = dataSnapshot.getValue(Data.class);

                                references.getChat().message(
                                        data.chatKey
                                ).child(data.key).child(Constant.STATUS)
                                        .addListenerForSingleValueEvent(
                                                new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot _dataSnapshot) {
                                                        if (_dataSnapshot.getValue() != null) {
                                                            if (_dataSnapshot.getValue(Integer.class) == Constant.MESSAGE_READ) {
                                                                dataSnapshot.getRef().setValue(null);
                                                                return;
                                                            }
                                                        }
                                                        datas.add(data);
                                                        updateStatus(dataSnapshot.getRef(), data);
                                                        sendNotification(datas, data);

                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }
                                                }
                                        );

                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {
                                Data _data = dataSnapshot.getValue(Data.class);
                                Data data = find(datas, _data);
//                                int index = datas.indexOf(data);
                                datas.remove(data);

                                if (datas.size() > 0)
                                    sendNotification(datas, data);
                                else {
                                    NotificationManager notificationManager =
                                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                    notificationManager.cancel(0);
                                }

                            }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        }
                );

        return inboxStyle;

    }

    private Data find(List<Data> datas, Data data) {
        for (Data _Data : datas) {
            if (_Data.chatKey.equals(data.chatKey)) {
                if (_Data.key.equals(data.key)) {
                    return _Data;
                }
            }
        }

        return null;
    }
}